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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.util.InitialExtractor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();

    @NotBlank
    @Pattern(regexp = "^[A-Za-z가-힣0-9_]{2,50}", message = "이름은 한글, 영문, 숫자, _만 사용하여 2~50자 사이로 입력해 주세요")
    @Column(length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String initial;

    @NotBlank
    @Size(min = 4, max = 20)
    @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$")
    @Column(unique = true, length = 20)
    private String loginId;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotNull
    @Enumerated(STRING)
    private Role role;

    @Valid
    @NotNull
    @Embedded
    private Address address;

    @Builder
    public Member(String name, String loginId, String password, String zipcode, String baseAddress, String detailAddress) {
        this.name = name;
        this.loginId = loginId;
        this.password = password;
        this.role = Role.USER;
        this.address = new Address(zipcode, baseAddress, detailAddress);
    }

    public void changePassword(String newPassword, PasswordEncoder passwordEncoder) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호가 비어있습니다");
        } else if (passwordEncoder.matches(newPassword, this.password)) {
            throw new IllegalArgumentException("비밀번호가 기존과 달라야 합니다");
        }

        this.password = passwordEncoder.encode(newPassword);
    }

    public void changeAddress(String newZipcode, String newBaseAddress, String newDetailAddress) {
        if (this.address.getZipcode().equals(newZipcode) && this.address.getBaseAddress().equals(newBaseAddress) && this.address.getDetailAddress().equals(newDetailAddress)) {
            return;
        }

        this.address = new Address(newZipcode, newBaseAddress, newDetailAddress);
    }

    @PrePersist
    @PreUpdate
    private void preHandler() {
        this.initial = InitialExtractor.extract(this.name);
    }
}
