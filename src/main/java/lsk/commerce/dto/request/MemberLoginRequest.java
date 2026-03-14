package lsk.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberLoginRequest(
        @NotBlank @Size(min = 4, max = 20)
        String loginId,
        @NotBlank @Size(min = 8, max = 20)
        String password
) {
}
