package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @Schema(example = "가요_001")
        @NotBlank @Size(max = 20)
        String name,
        @Schema(example = "가요")
        String parentName
) {
}
