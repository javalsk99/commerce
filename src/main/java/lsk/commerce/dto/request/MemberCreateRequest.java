package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record MemberCreateRequest(
        @Schema(example = "유저")
        @NotBlank(message = "이름은 필수입니다") @Size(message = "이름은 2자에서 50자 사이로 입력해 주세요", min = 2, max = 50)
        String name,
        @Schema(example = "test_id_001")
        @NotBlank(message = "아이디는 필수입니다") @Size(message = "아이디는 4자에서 20자 사이로 입력해 주세요", min = 4, max = 20)
        @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$")
        String loginId,
        @Schema(example = "12345678")
        @NotBlank(message = "비밀번호는 필수입니다") @Size(message = "비밀번호는 8자에서 20자 사이로 입력해 주세요", min = 8, max = 20)
        String password,
        @Schema(example = "Seoul")
        @NotBlank(message = "도시명은 필수입니다") @Size(message = "도시명은 50자 이하로 입력해 주세요", max = 50)
        String city,
        @Schema(example = "Gangnam")
        @NotBlank(message = "거리명은 필수입니다") @Size(message = "거리명은 50자 이하로 입력해 주세요", max = 50)
        String street,
        @Schema(example = "01234")
        @NotBlank(message = "우편번호는 필수입니다") @Size(message = "우편번호는 50자 이하로 입력해 주세요", max = 10)
        String zipcode
) {
}
