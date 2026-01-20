package lsk.commerce.domain.product;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.domain.Product;

import static lombok.AccessLevel.*;

@Entity
@DiscriminatorValue("A")
@Getter
@NoArgsConstructor(access = PUBLIC)
public class Album extends Product {

    private String artist;
    private String studio;

    public Album(String name, int price, int stockQuantity, String artist, String studio) {
        super(name, price, stockQuantity);
        this.artist = artist;
        this.studio = studio;
    }

    public Album(String name, int price, int stockQuantity, String artist, String studio, String currency) {
        super(name, price, stockQuantity, currency);
        this.artist = artist;
        this.studio = studio;
    }
}
