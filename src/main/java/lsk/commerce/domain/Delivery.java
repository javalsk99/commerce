package lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.*;
import static lombok.AccessLevel.*;
import static lsk.commerce.domain.DeliveryStatus.*;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Delivery {

    @Id @GeneratedValue
    @Column(name = "delivery_id")
    private Long id;

    @Enumerated(STRING)
    private DeliveryStatus deliveryStatus;

    @Column(name = "shipped_at")
    private LocalDateTime shippedDate;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredDate;

    @Embedded
    private Address address;

    //주문을 결제해야 배송 준비가 시작돼서 결제 대기 상태 추가
    public Delivery(Member member) {
        this.address = member.getAddress();
        this.deliveryStatus = WAITING;
    }

    public Delivery(Address address) {
        this.address = address;
        this.deliveryStatus = WAITING;
    }

    public void startDelivery(Order order) {
        order.getDelivery().setDeliveryStatus(SHIPPED);
        order.getDelivery().shippedDate = LocalDateTime.now();
    }

    public void completeDelivery(Order order) {
        order.getDelivery().setDeliveryStatus(DELIVERED);
        order.getDelivery().deliveredDate = LocalDateTime.now();
        order.setOrderStatus(OrderStatus.DELIVERED);
    }

    protected void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    //결제 api 추가 전, 테스트용
    public void testShipped() {
        this.deliveryStatus = SHIPPED;
    }

    //결제 api 추가 전, 테스트용
    public void testDelivered() {
        this.deliveryStatus = DELIVERED;
    }
}
