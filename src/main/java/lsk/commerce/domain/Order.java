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

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

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

    @NotNull @Min(100)
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
        validateParameters(member, delivery, orderProducts);

        Order order = new Order();

        order.addMember(member);
        order.addDelivery(delivery);
        order.totalAmount = 0;
        for (OrderProduct orderProduct : orderProducts) {
            order.addOrderProduct(orderProduct);
            order.totalAmount += orderProduct.getOrderPrice();
        }
        order.orderDate = LocalDateTime.now();
        order.orderStatus = OrderStatus.CREATED;
        order.orderNumber = NanoIdUtils.randomNanoId(SECURE_RANDOM, NUMBER_ALPHABET, 12);

        return order;
    }

    public void clearOrderProduct() {
        for (OrderProduct orderProduct : this.orderProducts) {
            orderProduct.getProduct().addStock(orderProduct.getCount());
        }

        this.totalAmount = 0;
        this.orderProducts.clear();
    }

    public Order updateOrder(List<OrderProduct> newOrderProducts) {
        validateOrderProducts(newOrderProducts);

        for (OrderProduct newOrderProduct : newOrderProducts) {
            this.addOrderProduct(newOrderProduct);
            this.totalAmount += newOrderProduct.getOrderPrice();
        }

        return this;
    }

    public void cancel() {
        if (this.orderStatus == OrderStatus.CANCELED) {
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

    protected void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    protected void setPayment(Payment payment) {
        this.payment = payment;
    }

    private static void validateParameters(Member member, Delivery delivery, List<OrderProduct> orderProducts) {
        if (member == null) {
            throw new IllegalArgumentException("주문할 회원이 없습니다.");
        }

        if (delivery == null) {
            throw new IllegalArgumentException("배송 정보가 없습니다.");
        }

        if (member.getAddress() == null || delivery.getAddress() == null) {
            throw new IllegalArgumentException("배송될 주소가 없습니다.");
        }

        if (orderProducts == null || orderProducts.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다.");
        }
    }

    private void validateOrderProducts(List<OrderProduct> newOrderProducts) {
        if (this.orderProducts == null) {
            throw new IllegalStateException("주문 상품이 없습니다.");
        } else if (!this.orderProducts.isEmpty()) {
            throw new IllegalStateException("주문 상품이 비어 있지 않습니다.");
        }

        if (newOrderProducts == null || newOrderProducts.isEmpty()) {
            throw new IllegalArgumentException("수정할 주문 상품이 없습니다.");
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
        if (this.payment == null) {
            throw new IllegalStateException("진행 중인 결제가 없습니다.");
        }

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
