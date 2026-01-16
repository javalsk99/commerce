package lsk.commerce.domain.product;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;

@Entity
@DiscriminatorValue("M")
@Getter
public class Movie extends Product {

    private String director;
    private String actor;
}
