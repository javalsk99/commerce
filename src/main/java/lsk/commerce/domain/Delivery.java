package lsk.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE delivery SET deleted = true WHERE delivery_id = ?")
public class Delivery {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "delivery_id")
    private Long id;

    @OneToOne(mappedBy = "delivery", fetch = LAZY, cascade = ALL)
    private Order order;

    @NotNull
    @Enumerated(STRING)
    private DeliveryStatus deliveryStatus;

    @Column(name = "shipped_at")
    private LocalDateTime shippedDate;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredDate;

    @Valid
    @NotNull
    @Embedded
    private Address address;

    @Column(nullable = false)
    private boolean deleted = false;

    protected void setOrder(Order order) {
        this.order = order;
    }

    //주문을 결제해야 배송 준비가 시작돼서 결제 대기 상태 추가
    public Delivery(Member member) {
        validateMemberAddress(member);

        this.address = member.getAddress();
        this.deliveryStatus = DeliveryStatus.WAITING;
    }

    public void startDelivery() {
        validatePaidOrderAndCompletedPayment();
        validateCanShip();

        this.deliveryStatus = DeliveryStatus.SHIPPED;
        this.shippedDate = LocalDateTime.now();
    }

    public void completeDelivery() {
        validatePaidOrderAndCompletedPayment();
        validateShipped();

        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveredDate = LocalDateTime.now();
        this.order.setOrderStatus(OrderStatus.DELIVERED);
    }

    private static void validateMemberAddress(Member member) {
        Address address = member.getAddress();
        if (address == null) {
            throw new IllegalArgumentException("회원의 주소 정보가 없습니다.");
        } else if (address.getCity() == null || address.getStreet() == null || address.getZipcode() == null) {
            throw new IllegalArgumentException("회원의 주소 정보가 잘못됐습니다. address.city = " + address.getCity() +
                    ", address.street = " + address.getStreet() + ", address.zipcode = " + address.getZipcode());
        }
    }

    private void validatePaidOrderAndCompletedPayment() {
        OrderStatus orderStatus = this.order.getOrderStatus();
        if (orderStatus == OrderStatus.CREATED || orderStatus == OrderStatus.CANCELED) {
            throw new IllegalStateException("결제 완료된 주문이 아닙니다. OrderStatus: " + orderStatus);
        } else if (orderStatus == OrderStatus.DELIVERED) {
            throw new IllegalStateException("이미 배송 완료된 주문입니다. OrderStatus: " + orderStatus);
        }

        if (this.order.getPayment() == null) {
            throw new IllegalStateException("주문의 결제 정보가 없습니다.");
        }

        PaymentStatus paymentStatus = this.order.getPayment().getPaymentStatus();
        if (paymentStatus != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("결제 완료된 주문이 아닙니다. PaymentStatus: " + paymentStatus);
        }
    }

    private void validateCanShip() {
        DeliveryStatus deliveryStatus = this.deliveryStatus;
        if (deliveryStatus == DeliveryStatus.WAITING || deliveryStatus == DeliveryStatus.CANCELED) {
            throw new IllegalStateException("결제 완료된 주문이 아닙니다. DeliveryStatus: " + deliveryStatus);
        } else if (deliveryStatus == DeliveryStatus.SHIPPED || deliveryStatus == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("이미 발송된 주문입니다. DeliveryStatus: " + deliveryStatus);
        }
    }

    private void validateShipped() {
        DeliveryStatus deliveryStatus = this.deliveryStatus;
        if (deliveryStatus == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("이미 배송 완료된 주문입니다. DeliveryStatus: " + deliveryStatus);
        } else if (deliveryStatus != DeliveryStatus.SHIPPED) {
            throw new IllegalStateException("발송된 주문이 아닙니다. DeliveryStatus: " + deliveryStatus);
        }
    }

    protected void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    protected void canceled() {
        this.deliveryStatus = DeliveryStatus.CANCELED;
    }
}
