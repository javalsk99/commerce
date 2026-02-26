package lsk.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.util.UUID.randomUUID;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE payment SET deleted = true WHERE payment_id = ?")
public class Payment {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @NotBlank @Size(min = 36, max = 36)
    @Column(name = "payment_number", length = 36)
    private String paymentId;

    @OneToOne(mappedBy = "payment", fetch = LAZY)
    private Order order;

    @NotNull @Min(100)
    private Integer paymentAmount;

    @Column(name = "paid_at")
    private LocalDateTime paymentDate;

    @NotNull
    @Enumerated(STRING)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private boolean deleted = false;

    public static void requestPayment(Order order) {
        validateStatusForRequest(order);

        Payment payment = new Payment();
        payment.paymentAmount = order.getTotalAmount();
        payment.paymentStatus = PaymentStatus.PENDING;
        payment.addOrder(order);
        payment.paymentId = randomUUID().toString();
    }

    private void addOrder(Order order) {
        this.order = order;
        order.setPayment(this);
    }

    public void failed() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public void canceled() {
        this.paymentStatus = PaymentStatus.CANCELED;
    }

    public void complete(LocalDateTime paymentDate) {
        validateStatusForComplete();

        this.paymentStatus = PaymentStatus.COMPLETED;
        this.paymentDate = paymentDate;
    }

    private static void validateStatusForRequest(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("주문 정보가 없습니다.");
        }

        if (order.getPayment() != null) {
            throw new IllegalStateException("이미 결제 정보가 있습니다.");
        }

        OrderStatus orderStatus = order.getOrderStatus();
        if (orderStatus == OrderStatus.CANCELED) {
            throw new IllegalStateException("취소된 주문은 결제할 수 없습니다. OrderStatus: " + orderStatus);
        }

        if (orderStatus != OrderStatus.CREATED) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다. OrderStatus: " + orderStatus);
        }

        DeliveryStatus deliveryStatus = order.getDelivery().getDeliveryStatus();
        if (deliveryStatus == DeliveryStatus.CANCELED) {
            throw new IllegalStateException("취소된 주문은 결제할 수 없습니다. DeliveryStatus: " + deliveryStatus);
        }

        if (deliveryStatus != DeliveryStatus.WAITING) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다. DeliveryStatus: " + deliveryStatus);
        }
    }

    private void validateStatusForComplete() {
        if (this.paymentStatus == PaymentStatus.CANCELED) {
            throw new IllegalStateException("취소된 주문은 결제할 수 없습니다.");
        }

        if (this.paymentStatus == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다.");
        }

        OrderStatus orderStatus = this.order.getOrderStatus();
        if (orderStatus == OrderStatus.CANCELED) {
            throw new IllegalStateException("취소된 주문은 결제할 수 없습니다. OrderStatus: " + orderStatus);
        }

        if (orderStatus == OrderStatus.PAID || orderStatus == OrderStatus.DELIVERED) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다. OrderStatus: " + orderStatus);
        }

        DeliveryStatus deliveryStatus = this.order.getDelivery().getDeliveryStatus();
        if (deliveryStatus == DeliveryStatus.CANCELED) {
            throw new IllegalStateException("취소된 주문은 결제할 수 없습니다. DeliveryStatus: " + deliveryStatus);
        }

        if (deliveryStatus == DeliveryStatus.PREPARING || deliveryStatus == DeliveryStatus.SHIPPED || deliveryStatus == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("이미 결제 완료된 주문입니다. DeliveryStatus: " + deliveryStatus);
        }
    }
}
