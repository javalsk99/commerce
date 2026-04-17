package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CategoryCreateRequest(
        @Schema(example = "가요_001")
        @NotBlank(message = "카테고리 이름은 필수입니다")
        @Pattern(regexp = "^[A-Za-z가-힣0-9 ()/_]{1,20}$", message = "카테고리 이름은 한글, 영문, 숫자, 공백, 특수문자(()/_)만 사용하여 1~20자 사이로 입력해 주세요")
        String name,
        @Schema(example = "K39smsEB598D")
        @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "부모 카테고리 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
        String parentNumber
) {
}
