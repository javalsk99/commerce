package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ProductCreateRequest(
        @Schema(example = "음악_001")
        @NotBlank @Size(max = 50)
        String name,
        @Schema(example = "15000")
        @NotNull @Min(100)
        Integer price,
        @Schema(example = "100")
        @NotNull @Min(0)
        Integer stockQuantity,

        @Schema(example = "A")
        @NotBlank @Size(min = 1, max = 1)
        String dtype,

        @Schema(example = "artist")
        @Size(max = 50)
        String artist,
        @Schema(example = "studio")
        @Size(max = 50)
        String studio,

        @Size(max = 50)
        String author,
        @Size(min = 10, max = 13)
        String isbn,

        @Size(max = 50)
        String actor,
        @Size(max = 50)
        String director
) {
    @Schema(hidden = true)
    @AssertTrue
    public boolean isValidFields() {
        if ("A".equals(dtype)) {
            return (artist != null && studio != null) &&
                    (author == null && isbn == null) &&
                    (actor == null && director == null);
        } else if ("B".equals(dtype)) {
            return (artist == null && studio == null) &&
                    (author != null && isbn != null) &&
                    (actor == null && director == null);
        } else if ("M".equals(dtype)) {
            return (artist == null && studio == null) &&
                    (author == null && isbn == null) &&
                    (actor != null && director != null);
        }

        return false;
    }
}
