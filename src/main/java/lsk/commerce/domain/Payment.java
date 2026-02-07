package lsk.commerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE payment SET deleted = true WHERE payment_id = ?")
public class Payment {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @NotBlank
    @Column(name = "payment_number", length = 36)
    private String paymentId;

    @OneToOne(mappedBy = "payment", fetch = LAZY)
    private Order order;

    @NotNull
    private Integer paymentAmount;

    @Column(name = "paid_at")
    private LocalDateTime paymentDate;

    @NotNull
    @Enumerated(STRING)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private boolean deleted = false;

    public static void requestPayment(Order order) {
        if (order.getOrderStatus() == OrderStatus.CANCELED) {
            throw new IllegalStateException("취소된 주문은 결제할 수 없습니다.");
        }

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
