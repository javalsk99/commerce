package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CategoryChangeParentRequest(
        @Schema(example = "dGzofETxzREd")
        @NotBlank(message = "부모 카테고리 번호는 필수입니다")
        @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "부모 카테고리 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
        String parentNumber
) {
}
