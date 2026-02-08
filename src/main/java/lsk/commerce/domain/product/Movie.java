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
@DiscriminatorValue("M")
@Getter
@NoArgsConstructor(access = PUBLIC)
public class Movie extends Product {

    @NotBlank
    @Column(length = 50)
    private String director;

    @NotBlank
    @Column(length = 50)
    private String actor;

    public Movie(String name, Integer price, Integer stockQuantity, String director, String actor) {
        super(name, price, stockQuantity);
        this.director = director;
        this.actor = actor;
    }
}
