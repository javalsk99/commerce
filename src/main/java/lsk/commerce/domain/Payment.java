package lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.FetchType.*;
import static lombok.AccessLevel.*;
import static lsk.commerce.domain.OrderStatus.*;
import static lsk.commerce.domain.PaymentStatus.*;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Payment {

    @Id @GeneratedValue
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "paid_id")
    private String paymentId; //결제 api용

    @OneToOne(mappedBy = "payment", fetch = LAZY)
    private Order order;

    private int paymentAmount;

    @Column(name = "paid_at")
    private LocalDateTime paymentDate;

    @Enumerated(STRING)
    private PaymentStatus paymentStatus;

    public static void requestPayment(Order order) {
        if (!order.getOrderStatus().equals(CREATED)) {
            throw new IllegalStateException("이미 결제된 주문입니다.");
        }

        Payment payment = new Payment();
        payment.paymentAmount = order.getTotalAmount();
        payment.paymentStatus = PENDING;
        payment.addOrder(order);
    }

    //Order에 payment를 넣기 위해 양방향 매핑 추가
    private void addOrder(Order order) {
        this.order = order;
        order.setPayment(this);
    }

    //결제 api용
    public Payment(String paymentId, PaymentStatus paymentStatus) {
        this.paymentId = paymentId;
        this.paymentStatus = paymentStatus;
    }

    //결제 api용
    public Payment(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    //결제 api용
    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    //결제 api 추가 전, 테스트용
    public void testFailed() {
        this.paymentStatus = FAILED;
    }

    //결제 api 추가 전, 테스트용
    public void testCompleted() {
        this.paymentStatus = COMPLETED;
    }
}
