package lsk.commerce.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.domain.Product;

import static lombok.AccessLevel.*;

@Entity
@DiscriminatorValue("A")
@Getter
@NoArgsConstructor(access = PUBLIC)
public class Album extends Product {

    @NotBlank
    @Column(length = 50)
    private String artist;

    @NotBlank
    @Column(length = 50)
    private String studio;

    public Album(String name, int price, int stockQuantity, String artist, String studio) {
        super(name, price, stockQuantity);
        this.artist = artist;
        this.studio = studio;
    }
}
