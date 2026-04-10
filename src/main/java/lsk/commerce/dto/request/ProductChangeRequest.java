package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

public record ProductChangeRequest(
        @Schema(example = "10000")
        @Min(value = 100, message = "상품 가격은 100원 이상이어야 합니다")
        Integer price,
        @Schema(example = "200")
        @Min(value = 0, message = "재고는 0개 이상이어야 합니다")
        Integer stockQuantity
) {
    @Schema(hidden = true)
    @AssertTrue(message = "상품 가격 또는 재고를 입력해 주세요")
    public boolean isValidFieldsPresent() {
        return price != null || stockQuantity != null;
    }
}
