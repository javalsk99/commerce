package lsk.commerce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record OrderCreateRequest(
        @NotBlank @Size(min = 4, max = 20)
        String memberLoginId,
        @NotEmpty
        Map<@NotBlank @Size(min = 12, max = 12) String, @NotNull @Min(0) @Max(100) Integer> productMap
) {
}
