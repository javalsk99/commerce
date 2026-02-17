package lsk.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberRequest {

    @NotBlank @Size(min = 2, max = 50)
    private String name;

    @NotBlank @Size(min = 4, max = 20)
    private String loginId;

    @NotBlank @Size(min = 8, max = 20)
    private String password;

    @NotBlank @Size(max = 50)
    private String city;

    @NotBlank @Size(max = 50)
    private String street;

    @NotBlank @Size(max = 10)
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
