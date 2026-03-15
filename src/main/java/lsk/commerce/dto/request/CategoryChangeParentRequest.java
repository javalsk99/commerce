package lsk.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryChangeParentRequest(
        @NotBlank @Size(max = 20)
        String name
) {
}
