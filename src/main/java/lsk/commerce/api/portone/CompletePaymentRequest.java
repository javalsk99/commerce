package lsk.commerce.api.portone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompletePaymentRequest(
        @NotBlank @Size(min = 12, max = 12)
        String paymentId
) {
}
