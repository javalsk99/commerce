package lsk.commerce.domain;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;
import static lsk.commerce.domain.OrderStatus.CREATED;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = PROTECTED)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE orders SET deleted = true WHERE order_id = ?")
public class Order {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] NUMBER_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnoqprstuvwxyz".toCharArray();

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @NotBlank @Size(min = 12, max = 12)
    @Column(length = 12)
    private String orderNumber;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @NotNull
    @OneToOne(fetch = LAZY, cascade = ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @OneToOne(fetch = LAZY, cascade = ALL)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    //양방향 매핑으로 변경
    @OneToMany(mappedBy = "order")
    private List<OrderProduct> orderProducts = new ArrayList<>();

    @NotNull @Min(0)
    private Integer totalAmount;

    @NotNull
    @Column(name = "ordered_at")
    private LocalDateTime orderDate;

    @NotNull
    @Enumerated(STRING)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private boolean deleted = false;

    private void addDelivery(Delivery delivery) {
        this.delivery = delivery;
        this.delivery.setOrder(this);
    }

    private void addMember(Member member) {
        this.member = member;
        this.member.getOrders().add(this);
    }

    //OrderProduct에 order를 넣기 위해 양방향 매핑 추가
    private void addOrderProduct(OrderProduct orderProduct) {
        orderProducts.add(orderProduct);
        orderProduct.setOrder(this);
    }

    //결제는 주문 생성하고 바로 진행하지 않는다.
    public static Order createOrder(Member member, Delivery delivery, List<OrderProduct> orderProducts) {
        validateAddress(member, delivery);

        Order order = new Order();

        order.addMember(member);
        order.addDelivery(delivery);

        int calculatedPrice = 0;
        for (OrderProduct orderProduct : orderProducts) {
            order.addOrderProduct(orderProduct);
            calculatedPrice += orderProduct.getOrderPrice();
        }
        order.totalAmount = calculatedPrice;
        order.orderDate = LocalDateTime.now();
        order.orderStatus = OrderStatus.CREATED;
        order.orderNumber = NanoIdUtils.randomNanoId(SECURE_RANDOM, NUMBER_ALPHABET, 12);

        return order;
    }

    public void clearOrderProduct() {
        validateStatusForClear();

        for (OrderProduct orderProduct : this.orderProducts) {
            orderProduct.getProduct().addStock(orderProduct.getCount());
        }

        this.totalAmount = 0;
        this.orderProducts.clear();
    }

    public Map<String, Integer> getOrderProductsAsMap() {
        return this.orderProducts.stream()
                .collect(Collectors.toMap(
                        op -> op.getProductName(),
                        op -> op.getCount()));
    }

    public Order updateOrder(List<OrderProduct> newOrderProducts) {
        int calculatedPrice = 0;
        for (OrderProduct newOrderProduct : newOrderProducts) {
            this.addOrderProduct(newOrderProduct);
            calculatedPrice += newOrderProduct.getOrderPrice();
        }
        this.totalAmount = calculatedPrice;

        return this;
    }

    public void cancel() {
        if (this.orderStatus == OrderStatus.CANCELED || this.delivery.getDeliveryStatus() == DeliveryStatus.CANCELED) {
            return;
        }

        validateStatusForCancel();

        for (OrderProduct orderProduct : this.orderProducts) {
            orderProduct.getProduct().addStock(orderProduct.getCount());
        }

        this.getDelivery().canceled();
        this.orderStatus = OrderStatus.CANCELED;
    }

    public void completePaid() {
        if (this.orderStatus == OrderStatus.PAID && this.delivery.getDeliveryStatus() == DeliveryStatus.PREPARING) {
            return;
        }

        validateStatusForCompletePaid();

        this.orderStatus = OrderStatus.PAID;
        this.delivery.setDeliveryStatus(DeliveryStatus.PREPARING);
    }

    public void validateDeletable() {
        if (this.getOrderStatus() == OrderStatus.CREATED) {
            throw new IllegalStateException("주문을 취소해야 삭제할 수 있습니다.");
        } else if (this.getOrderStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("배송이 완료돼야 삭제할 수 있습니다.");
        }

        DeliveryStatus deliveryStatus = this.getDelivery().getDeliveryStatus();
        if (deliveryStatus == DeliveryStatus.WAITING) {
            throw new IllegalStateException("주문을 취소해야 삭제할 수 있습니다. DeliveryStatus: " + deliveryStatus);
        } else if (deliveryStatus == DeliveryStatus.PREPARING || deliveryStatus == DeliveryStatus.SHIPPED) {
            throw new IllegalStateException("배송이 완료돼야 삭제할 수 있습니다. DeliveryStatus: " + deliveryStatus);
        }

        if (this.getPayment() != null) {
            PaymentStatus paymentStatus = this.getPayment().getPaymentStatus();
            if (paymentStatus == PaymentStatus.PENDING || paymentStatus == PaymentStatus.FAILED) {
                throw new IllegalStateException("주문을 취소해야 삭제할 수 있습니다. PaymentStatus: " + paymentStatus);
            }
        }
    }

    protected void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    protected void setPayment(Payment payment) {
        this.payment = payment;
    }

    private static void validateAddress(Member member, Delivery delivery) {
        if (member.getAddress() == null || delivery.getAddress() == null) {
            throw new IllegalArgumentException("배송될 주소가 없습니다.");
        }
    }

    private void validateStatusForClear() {
        if (this.getOrderStatus() != CREATED) {
            throw new IllegalStateException("주문 생성 상태가 아니어서 주문 상품을 비울 수 없습니다.");
        }

        if (this.payment != null) {
            if (this.payment.getPaymentStatus() != PaymentStatus.PENDING) {
                throw new IllegalStateException("결제 대기 상태가 아니어서 주문 상품을 비울 수 없습니다.");
            }
        }

        if (this.delivery.getDeliveryStatus() != DeliveryStatus.WAITING) {
            throw new IllegalStateException("배송 대기 상태가 아니어서 주문 상품을 비울 수 없습니다.");
        }
    }

    private void validateStatusForCancel() {
        if (this.orderStatus != OrderStatus.CREATED) {
            throw new IllegalStateException("결제 완료된 주문이어서 취소할 수 없습니다.");
        }

        if (this.payment != null) {
            if (this.payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
                throw new IllegalStateException("결제 완료돼서 취소할 수 없습니다.");
            }

            this.payment.canceled();
        }

        if (this.delivery.getDeliveryStatus() != DeliveryStatus.WAITING) {
            throw new IllegalStateException("배송 대기 상태가 아니여서 취소할 수 없습니다.");
        }
    }

    private void validateStatusForCompletePaid() {
        if (this.payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("결제가 완료되지 않았습니다.");
        }

        if (this.orderStatus != OrderStatus.CREATED) {
            throw new IllegalStateException("결제 완료 처리가 불가능한 주문입니다.");
        }

        if (this.delivery.getDeliveryStatus() != DeliveryStatus.WAITING) {
            throw new IllegalStateException("배송 대기 상태가 아닙니다.");
        }
    }
}
