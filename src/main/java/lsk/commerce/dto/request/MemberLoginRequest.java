package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberLoginRequest(
        @Schema(example = "testId")
        @NotBlank(message = "아이디는 필수입니다")
        String loginId,
        @Schema(example = "testPassword")
        @NotBlank(message = "비밀번호는 필수입니다")
        String password
) {
}
