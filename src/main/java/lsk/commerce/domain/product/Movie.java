package lsk.commerce.domain.product;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.*;

@Entity
@DiscriminatorValue("M")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Movie extends Product {

    private String director;
    private String actor;

    public Movie(String name, int price, int stockQuantity, String director, String actor) {
        super(name, price, stockQuantity);
        this.director = director;
        this.actor = actor;
    }
}
