package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberChangePasswordRequest(
        @Schema(example = "00000000")
        @NotBlank @Size(min = 8, max = 20)
        String password
) {
}
