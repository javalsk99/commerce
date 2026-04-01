package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberChangeAddressRequest(
        @Schema(example = "Gyeonggi-do")
        @NotBlank @Size(max = 50)
        String city,
        @Schema(example = "Gangbuk")
        @NotBlank @Size(max = 50)
        String street,
        @Schema(example = "01235")
        @NotBlank @Size(max = 10)
        String zipcode
) {
}
