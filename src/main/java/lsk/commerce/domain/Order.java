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
import static lsk.commerce.domain.DeliveryStatus.PREPARING;
import static lsk.commerce.domain.OrderStatus.CANCELED;
import static lsk.commerce.domain.OrderStatus.PAID;
import static lsk.commerce.domain.PaymentStatus.COMPLETED;

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

    public static Order updateOrder(Order order, List<OrderProduct> newOrderProducts) {
        order.totalAmount = 0;
        for (OrderProduct newOrderProduct : newOrderProducts) {
            order.addOrderProduct(newOrderProduct);
            order.totalAmount += newOrderProduct.getOrderPrice();
        }

        return order;
    }

    public void completePaid() {
        this.orderStatus = PAID;
        this.delivery.setDeliveryStatus(PREPARING);
    }

    protected void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    //Payment에서 사용해서 protected
    protected void setPayment(Payment payment) {
        this.payment = payment;
    }

    public void cancel() {
        if (this.orderStatus == CANCELED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }

        for (OrderProduct orderProduct : this.orderProducts) {
            orderProduct.getProduct().addStock(orderProduct.getCount());
        }

        if (this.payment != null) {
            if (this.payment.getPaymentStatus() == COMPLETED) {
                throw new IllegalStateException("결제가 완료돼서 취소할 수 없습니다.");
            }

            this.payment.canceled();
        }

        this.getDelivery().canceled();
        this.orderStatus = CANCELED;
    }
}
