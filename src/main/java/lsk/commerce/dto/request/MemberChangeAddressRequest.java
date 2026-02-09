package lsk.commerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class MemberChangeAddressRequest {

    @NotBlank @Size(max = 50)
    private String city;

    @NotBlank @Size(max = 50)
    private String street;

    @NotBlank @Size(max = 10)
    private String zipcode;

    public MemberChangeAddressRequest(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
