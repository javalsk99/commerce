package lsk.commerce.api.portone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompletePaymentRequest(
        @NotBlank @Size(min = 36, max = 36)
        String paymentId
) {
}
