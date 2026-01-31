package lsk.commerce.domain;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.FetchType.*;
import static lombok.AccessLevel.*;
import static lsk.commerce.domain.DeliveryStatus.*;
import static lsk.commerce.domain.OrderStatus.CANCELED;
import static lsk.commerce.domain.OrderStatus.PAID;
import static lsk.commerce.domain.PaymentStatus.*;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = PROTECTED)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE orders SET deleted = true WHERE order_id = ?")
public class Order {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] NUMBER_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnoqprstuvwxyz".toCharArray();

    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    private String orderNumber;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToOne(fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    //양방향 매핑으로 변경
    @OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true)
    private List<OrderProduct> orderProducts = new ArrayList<>();

    private int totalAmount;

    @Column(name = "ordered_at")
    private LocalDateTime orderDate;

    @Enumerated(STRING)
    private OrderStatus orderStatus;

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
        for (OrderProduct orderProduct : this.orderProducts) {
            orderProduct.getProduct().addStock(orderProduct.getCount());
        }

        if (this.payment != null) {
            if (this.payment.getPaymentStatus() == COMPLETED) {
                throw new IllegalStateException("결제가 완료돼서 취소할 수 없습니다.");
            }

            this.payment.canceled();
        }

        this.orderStatus = CANCELED;
    }
}
