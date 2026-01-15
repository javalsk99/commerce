package Java.lsk.commerce.domain.product;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;

@Entity
@DiscriminatorValue("B")
@Getter
public class Book extends Product {

    private String author;
    private String isbn;
}
