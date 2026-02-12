package lsk.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;

@Getter
public class PaymentRequest {

    @NotBlank @Size(min = 12, max = 12)
    private String paymentId;

    @NotNull
    private PaymentStatus paymentStatus;

    public PaymentRequest(String paymentId, PaymentStatus paymentStatus) {
        this.paymentId = paymentId;
        this.paymentStatus = paymentStatus;
    }

    public static PaymentRequest paymentChangeDto(Payment payment) {
        return new PaymentRequest(payment.getPaymentId(), payment.getPaymentStatus());
    }
}
