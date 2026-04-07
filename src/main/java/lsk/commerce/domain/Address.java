package lsk.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Embeddable
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Address {

    @NotBlank
    @Pattern(regexp = "^\\d{5}$", message = "우편번호는 숫자 5자로 입력해 주세요")
    @Column(nullable = false, length = 5)
    private String zipcode;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z가-힣0-9 -]{1,50}$", message = "기본 주소는 한글, 영문, 숫자, -, 공백만 사용하여 1~50자 사이로 입력해 주세요")
    @Column(nullable = false, length = 50)
    private String baseAddress;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z가-힣0-9 ().,-]{1,100}$", message = "상세 주소는 한글, 영문, 숫자, 특수문자(().,-), 공백만 사용하여 1~100자 사이로 입력해 주세요")
    @Column(nullable = false, length = 100)
    private String detailAddress;

    public Address(String zipcode, String baseAddress, String detailAddress) {
        this.zipcode = zipcode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
    }
}
