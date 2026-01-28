package lsk.commerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MemberChangeAddressRequest {

    @NotNull
    private String city;

    @NotNull
    private String street;

    @NotNull
    private String zipcode;

    public MemberChangeAddressRequest(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
