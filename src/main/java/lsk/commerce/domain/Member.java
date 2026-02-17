package lsk.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.util.InitialExtractor;

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

    @Column(nullable = false, length = 50)
    private String initial;

    @NotBlank @Size(min = 4, max = 20)
    @Column(unique = true, length = 20)
    private String loginId;

    @NotBlank @Size(min = 8)
    private String password;

    @NotNull
    @Enumerated(STRING)
    private Grade grade;

    @Valid
    @NotNull
    @Embedded
    private Address address;

    @Builder
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

    public void changePassword(String newEncodedPassword) {
        if (!newEncodedPassword.startsWith("$2a$")) {
            throw new IllegalArgumentException("암호화되지 않은 비밀번호입니다.");
        }

        this.password = newEncodedPassword;
    }

    public void changeAddress(String newCity, String newStreet, String newZipcode) {
        if (this.address.getCity().equals(newCity) && this.address.getStreet().equals(newStreet) && this.address.getZipcode().equals(newZipcode)) {
            throw new IllegalArgumentException("주소가 기존과 달라야 합니다.");
        }

        this.address = new Address(newCity, newStreet, newZipcode);
    }

    @PrePersist
    @PreUpdate
    private void preHandler() {
        this.initial = InitialExtractor.extract(this.name);
    }
}
