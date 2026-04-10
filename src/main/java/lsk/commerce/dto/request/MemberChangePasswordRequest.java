package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MemberChangePasswordRequest(
        @Schema(example = "cdCD34#$")
        @NotBlank(message = "비밀번호는 필수입니다")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#$%^&*()_+=-])[A-Za-z0-9!@#$%^&*()_+=-]{8,20}$", message = "영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요")
        String password
) {
}
