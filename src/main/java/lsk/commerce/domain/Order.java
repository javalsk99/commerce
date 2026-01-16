package lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.FetchType.*;

@Entity
@Table(name = "orders")
@Getter
public class Order {

    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

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
}
