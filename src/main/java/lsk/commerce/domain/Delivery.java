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

    private LocalDateTime shippedDate;
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

    public static void startShipping(Order order) {
        if (!order.getPayment().getPaymentStatus().equals(PaymentStatus.COMPLETED)) {
            throw new IllegalStateException("결제가 완료된 주문이 아닙니다.");
        } else if (order.getDelivery().deliveryStatus.equals(SHIPPED) || order.getDelivery().deliveryStatus.equals(DELIVERED)) {
            throw new IllegalStateException("이미 배송된 주문입니다.");
        }

        order.getDelivery().setDeliveryStatus(PREPARING);
    }

    private void setDeliveryStatus(DeliveryStatus deliveryStatus) {
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
