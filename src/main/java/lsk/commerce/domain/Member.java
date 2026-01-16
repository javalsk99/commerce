package lsk.commerce.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.*;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    @NotNull
    private String name;
    private String loginId;
    private String password;

    @Embedded
    private Address address;

    public Member(String name, String loginId, String password, String city, String street, String zipcode) {
        this.name = name;
        this.loginId = loginId;
        this.password = password;
        this.address = new Address(city, street, zipcode);
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void changeAddress(String newCity, String newStreet, String newZipcode) {
        this.address = new Address(newCity, newStreet, newZipcode);
    }
}
