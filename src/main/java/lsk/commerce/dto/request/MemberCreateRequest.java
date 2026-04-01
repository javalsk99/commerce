package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record MemberCreateRequest(
        @Schema(example = "유저")
        @NotBlank @Size(min = 2, max = 50)
        String name,
        @Schema(example = "test_id_001")
        @NotBlank @Size(min = 4, max = 20)
        @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$")
        String loginId,
        @Schema(example = "12345678")
        @NotBlank @Size(min = 8, max = 20)
        String password,
        @Schema(example = "Seoul")
        @NotBlank @Size(max = 50)
        String city,
        @Schema(example = "Gangnam")
        @NotBlank @Size(max = 50)
        String street,
        @Schema(example = "01234")
        @NotBlank @Size(max = 10)
        String zipcode
) {
}
