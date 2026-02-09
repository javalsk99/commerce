package lsk.commerce.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;
import static lsk.commerce.domain.Grade.ADMIN;
import static lsk.commerce.domain.Grade.USER;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Member {

    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();

    @NotBlank @Size(min = 2, max = 50)
    @Column(length = 50)
    private String name;

    @NotBlank @Size(min = 4, max = 20)
    @Column(unique = true, length = 20)
    private String loginId;

    @NotBlank @Size(min = 8, max = 20)
    private String password;

    @NotNull
    @Enumerated(STRING)
    private Grade grade;

    @Valid
    @NotNull
    @Embedded
    private Address address;

    public Member(String name, String loginId, String password, String city, String street, String zipcode) {
        this.name = name;
        this.loginId = loginId;
        this.password = password;
        this.grade = USER;
        this.address = new Address(city, street, zipcode);
    }

    public void setAdmin() {
        this.grade = ADMIN;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void changeAddress(String newCity, String newStreet, String newZipcode) {
        this.address = new Address(newCity, newStreet, newZipcode);
    }
}
