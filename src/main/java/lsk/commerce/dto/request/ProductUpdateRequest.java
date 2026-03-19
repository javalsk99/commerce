package lsk.commerce.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

public record ProductUpdateRequest(
        @Min(100)
        Integer price,
        @Min(0)
        Integer stockQuantity
) {
    @AssertTrue
    public boolean isValidFieldsPresent() {
        return price != null || stockQuantity != null;
    }
}
