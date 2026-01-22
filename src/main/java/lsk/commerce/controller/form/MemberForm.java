package lsk.commerce.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lsk.commerce.domain.Member;

@Getter
public class MemberForm {

    @NotNull
    private String name;

    @NotNull
    private String loginId;

    @JsonIgnore
    @NotNull
    private String password;

    @NotNull
    private String city;

    @NotNull
    private String street;

    @NotNull
    private String zipcode;

    public MemberForm(String name, String loginId, String password, String city, String street, String zipcode) {
        this.name = name;
        this.loginId = loginId;
        this.password = password;
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }

    public static MemberForm memberChangeForm(Member member) {
        return new MemberForm(member.getName(), member.getLoginId(), member.getPassword(), member.getAddress().getCity(), member.getAddress().getStreet(), member.getAddress().getZipcode());
    }
}
