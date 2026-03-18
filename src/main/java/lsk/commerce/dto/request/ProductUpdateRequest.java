package lsk.commerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductUpdateRequest(
        @NotNull @Min(100)
        Integer price,
        @NotNull @Min(0)
        Integer stockQuantity
) {
}
