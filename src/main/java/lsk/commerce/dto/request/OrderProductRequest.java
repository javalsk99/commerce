package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderProductRequest(
        @Schema(example = "WxgG3CzGZhAZ")
        @NotBlank @Size(min = 12, max = 12)
        String productNumber,
        @Schema(example = "3")
        @NotNull @Min(0) @Max(100)
        Integer quantity
) {
}
