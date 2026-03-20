package lsk.commerce.dto.request;

import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;

public record PaymentCompleteResponse(
        String paymentId,
        PaymentStatus paymentStatus
) {
    public static PaymentCompleteResponse from(Payment payment) {
        return new PaymentCompleteResponse(
                payment.getPaymentId(),
                payment.getPaymentStatus()
        );
    }
}
