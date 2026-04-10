package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OrderProductRequest(
        @Schema(example = "WxgG3CzGZhAZ")
        @NotBlank(message = "상품 번호는 필수입니다")
        @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
        String productNumber,
        @Schema(example = "3")
        @NotNull(message = "주문 수량은 필수입니다") @Min(value = 1, message = "주문 수량은 1개 이상이어야 합니다") @Max(value = 100, message = "주문 수량은 100개 이하여야 합니다")
        Integer quantity
) {
}
