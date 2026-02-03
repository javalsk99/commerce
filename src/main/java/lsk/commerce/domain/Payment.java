package lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.FetchType.*;
import static jakarta.persistence.GenerationType.*;
import static java.util.UUID.randomUUID;
import static lombok.AccessLevel.*;
import static lsk.commerce.domain.PaymentStatus.*;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Payment {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "payment_number")
    private String paymentId;

    @OneToOne(mappedBy = "payment", fetch = LAZY)
    private Order order;

    private int paymentAmount;

    @Column(name = "paid_at")
    private LocalDateTime paymentDate;

    @Enumerated(STRING)
    private PaymentStatus paymentStatus;

    public static void requestPayment(Order order) {
        if (order.getOrderStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("이미 결제된 주문입니다.");
        }

        Payment payment = new Payment();
        payment.paymentAmount = order.getTotalAmount();
        payment.paymentStatus = PENDING;
        payment.addOrder(order);
        payment.paymentId = randomUUID().toString();
    }

    //Order에 payment를 넣기 위해 양방향 매핑 추가
    private void addOrder(Order order) {
        this.order = order;
        order.setPayment(this);
    }

    public void failed() {
        this.paymentStatus = FAILED;
    }

    public void canceled() {
        this.paymentStatus = CANCELED;
    }

    public void complete(LocalDateTime paymentDate) {
        this.paymentStatus = COMPLETED;
        this.paymentDate = paymentDate;
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
