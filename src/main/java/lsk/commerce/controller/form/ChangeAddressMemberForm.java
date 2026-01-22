package lsk.commerce.controller.form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChangeAddressMemberForm {

    @NotNull
    private String city;

    @NotNull
    private String street;

    @NotNull
    private String zipcode;

    public ChangeAddressMemberForm(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
