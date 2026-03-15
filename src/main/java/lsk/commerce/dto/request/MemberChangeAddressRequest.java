package lsk.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberChangeAddressRequest(
        @NotBlank @Size(max = 50)
        String city,
        @NotBlank @Size(max = 50)
        String street,
        @NotBlank @Size(max = 10)
        String zipcode
) {
}
