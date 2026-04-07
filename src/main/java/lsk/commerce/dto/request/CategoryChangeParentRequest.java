package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CategoryChangeParentRequest(
        @Schema(example = "IT")
        @NotBlank
        @Pattern(regexp = "^[A-Za-z가-힣0-9_]{1,20}$", message = "부모 카테고리 이름은 한글, 영문, 숫자, _만 사용하여 1~20자 사이로 입력해 주세요")
        String parentName
) {
}
