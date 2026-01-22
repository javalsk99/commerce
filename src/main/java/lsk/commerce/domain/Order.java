package lsk.commerce.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.FetchType.*;
import static lombok.AccessLevel.*;
import static lsk.commerce.domain.OrderStatus.PAID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Order {

    @JsonIgnore
    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToOne(fetch = LAZY, cascade = ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    //양방향 매핑으로 변경
    @OneToMany(mappedBy = "order", cascade = ALL)
    private List<OrderProduct> orderProducts = new ArrayList<>();

    private int totalAmount;
    private LocalDateTime orderDate;

    @Enumerated(STRING)
    private OrderStatus orderStatus;

    //OrderProduct에 order를 넣기 위해 양방향 매핑 추가
    private void addOrderProduct(OrderProduct orderProduct) {
        orderProducts.add(orderProduct);
        orderProduct.setOrder(this);
    }

    //결제는 주문 생성하고 바로 진행하지 않는다.
    public static Order createOrder(Member member, Delivery delivery, List<OrderProduct> orderProducts) {
        Order order = new Order();

        order.member = member;
        order.delivery = delivery;
        order.totalAmount = 0;
        for (OrderProduct orderProduct : orderProducts) {
            order.addOrderProduct(orderProduct);
            order.totalAmount += orderProduct.getOrderPrice();
        }
        order.orderDate = LocalDateTime.now();
        order.orderStatus = OrderStatus.CREATED;

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

    //Payment에서 사용해서 protected
    protected void setPayment(Payment payment) {
        this.payment = payment;
    }

    private void setDelivery(Delivery delivery) {
        this.delivery = delivery;
    }

    //결제 api 추가 전, 테스트용
    public void testPaid() {
        this.orderStatus = PAID;
    }
}
