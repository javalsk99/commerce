package lsk.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;
import static lsk.commerce.domain.DeliveryStatus.CANCELED;
import static lsk.commerce.domain.DeliveryStatus.DELIVERED;
import static lsk.commerce.domain.DeliveryStatus.SHIPPED;
import static lsk.commerce.domain.DeliveryStatus.WAITING;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE delivery SET deleted = true WHERE delivery_id = ?")
public class Delivery {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "delivery_id")
    private Long id;

    @OneToOne(mappedBy = "delivery", fetch = LAZY, cascade = ALL)
    private Order order;

    @NotNull
    @Enumerated(STRING)
    private DeliveryStatus deliveryStatus;

    @Column(name = "shipped_at")
    private LocalDateTime shippedDate;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredDate;

    @Valid
    @NotNull
    @Embedded
    private Address address;

    @Column(nullable = false)
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
        validatePaidOrder();
        validateCanShip();

        this.deliveryStatus = SHIPPED;
        this.shippedDate = LocalDateTime.now();
    }

    public void completeDelivery() {
        validatePaidOrder();
        validateShipped();

        this.deliveryStatus = DELIVERED;
        this.deliveredDate = LocalDateTime.now();
        this.order.setOrderStatus(OrderStatus.DELIVERED);
    }

    private void validatePaidOrder() {
        if (this.order.getOrderStatus() == OrderStatus.CREATED || this.order.getOrderStatus() == OrderStatus.CANCELED) {
            throw new IllegalStateException("결제가 완료된 주문이 아닙니다.");
        } else if (this.order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("이미 배송 완료된 주문입니다.");
        }
    }

    private void validateCanShip() {
        if (this.deliveryStatus == WAITING) {
            throw new IllegalStateException("결제가 완료된 주문이 아닙니다.");
        } else if (this.deliveryStatus == CANCELED) {
            throw new IllegalStateException("취소된 주문입니다.");
        } else if (this.deliveryStatus == SHIPPED || this.deliveryStatus == DELIVERED) {
            throw new IllegalStateException("이미 발송된 주문입니다.");
        }
    }

    private void validateShipped() {
        if (this.deliveryStatus == DELIVERED) {
            throw new IllegalStateException("이미 배송 완료된 주문입니다.");
        } else if (this.deliveryStatus != SHIPPED) {
            throw new IllegalStateException("발송된 주문이 아닙니다.");
        }
    }

    protected void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    protected void canceled() {
        this.deliveryStatus = CANCELED;
    }
}
