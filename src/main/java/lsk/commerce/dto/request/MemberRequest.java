package lsk.commerce.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MemberRequest {

    @NotNull
    private String name;

    @NotNull
    private String loginId;

    @NotNull
    private String password;

    @NotNull
    private String city;

    @NotNull
    private String street;

    @NotNull
    private String zipcode;

    public MemberRequest(String name, String loginId, String password, String city, String street, String zipcode) {
        this.name = name;
        this.loginId = loginId;
        this.password = password;
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
