package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberLoginRequest(
        @Schema(example = "testId")
        @NotBlank @Size(min = 4, max = 20)
        String loginId,
        @Schema(example = "testPassword")
        @NotBlank @Size(min = 8, max = 20)
        String password
) {
}
