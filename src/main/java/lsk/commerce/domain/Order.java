package lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.FetchType.*;
import static lombok.AccessLevel.*;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Order {

    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    //양방향 매핑으로 변경
    @OneToMany(mappedBy = "order", cascade = ALL)
    private List<OrderProduct> orderProducts = new ArrayList<>();

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    private int totalAmount;
    private LocalDateTime orderDate;

    @Enumerated(STRING)
    private OrderStatus orderStatus;

    public void addOrderProduct(OrderProduct orderProduct) {
        orderProducts.add(orderProduct);
        orderProduct.setOrder(this);
    }

    //결제는 주문 생성하고 바로 진행하지 않는다.
    public static Order CreateOrder(Member member, Delivery delivery, List<OrderProduct> orderProducts) {
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
}
