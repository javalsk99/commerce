package lsk.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record MemberCreateRequest(
        @NotBlank @Size(min = 2, max = 50)
        String name,
        @NotBlank @Size(min = 4, max = 20)
        @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$")
        String loginId,
        @NotBlank @Size(min = 8, max = 20)
        String password,
        @NotBlank @Size(max = 50)
        String city,
        @NotBlank @Size(max = 50)
        String street,
        @NotBlank @Size(max = 10)
        String zipcode
) {
}
