package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryChangeParentRequest(
        @Schema(example = "컴퓨터/IT")
        @NotBlank @Size(max = 20)
        String name
) {
}
