package lsk.commerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ProductRequest(
        @NotBlank @Size(max = 50)
        String name,
        @NotNull @Min(100)
        Integer price,
        @NotNull @Min(0)
        Integer stockQuantity,
        @NotBlank @Size(min = 1, max = 1)
        String dtype,
        String artist,
        String studio,
        String author,
        String isbn,
        String actor,
        String director
) {
}
