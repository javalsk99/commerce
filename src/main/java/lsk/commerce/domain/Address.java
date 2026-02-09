package lsk.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Embeddable
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Address {

    @NotBlank @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String city;

    @NotBlank @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String street;

    @NotBlank @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String zipcode;

    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
