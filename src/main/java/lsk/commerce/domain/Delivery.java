package lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.FetchType.*;
import static lombok.AccessLevel.*;
import static lsk.commerce.domain.DeliveryStatus.*;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE delivery SET deleted = true WHERE delivery_id = ?")
public class Delivery {

    @Id @GeneratedValue
    @Column(name = "delivery_id")
    private Long id;

    @OneToOne(mappedBy = "delivery", fetch = LAZY, cascade = ALL)
    private Order order;

    @Enumerated(STRING)
    private DeliveryStatus deliveryStatus;

    @Column(name = "shipped_at")
    private LocalDateTime shippedDate;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredDate;

    @Embedded
    private Address address;

    private boolean deleted = false;

    protected void setOrder(Order order) {
        this.order = order;
    }

    //주문을 결제해야 배송 준비가 시작돼서 결제 대기 상태 추가
    public Delivery(Member member) {
        this.address = member.getAddress();
        this.deliveryStatus = WAITING;
    }

    public Delivery(Address address) {
        this.address = address;
        this.deliveryStatus = WAITING;
    }

    public void startDelivery() {
        this.deliveryStatus = SHIPPED;
        this.shippedDate = LocalDateTime.now();
    }

    public void completeDelivery() {
        this.deliveryStatus = DELIVERED;
        this.deliveredDate = LocalDateTime.now();
        this.order.setOrderStatus(OrderStatus.DELIVERED);
    }

    protected void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }
}
