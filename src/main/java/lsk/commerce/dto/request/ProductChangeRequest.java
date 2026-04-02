package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

public record ProductChangeRequest(
        @Schema(example = "10000")
        @Min(100)
        Integer price,
        @Schema(example = "200")
        @Min(0)
        Integer stockQuantity
) {
    @AssertTrue
    public boolean isValidFieldsPresent() {
        return price != null || stockQuantity != null;
    }
}
