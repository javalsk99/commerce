package lsk.commerce.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.domain.Product;

import static lombok.AccessLevel.*;

@Entity
@DiscriminatorValue("A")
@Getter
@NoArgsConstructor(access = PUBLIC)
public class Album extends Product {

    @NotBlank @Size(min = 1, max = 50)
    @Column(length = 50)
    private String artist;

    @NotBlank @Size(min = 1, max = 50)
    @Column(length = 50)
    private String studio;

    public Album(String name, Integer price, Integer stockQuantity, String artist, String studio) {
        super(name, price, stockQuantity);
        this.artist = artist;
        this.studio = studio;
    }
}
