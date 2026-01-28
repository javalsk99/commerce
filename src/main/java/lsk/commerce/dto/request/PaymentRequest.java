package lsk.commerce.dto.request;

import lombok.Getter;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;

@Getter
public class PaymentRequest {

    private String paymentId;
    private PaymentStatus paymentStatus;

    public PaymentRequest(String paymentId, PaymentStatus paymentStatus) {
        this.paymentId = paymentId;
        this.paymentStatus = paymentStatus;
    }

    public static PaymentRequest paymentChangeDto(Payment payment) {
        return new PaymentRequest(payment.getPaymentId(), payment.getPaymentStatus());
    }
}
