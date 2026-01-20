package lsk.commerce.domain.product;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.domain.Product;

import static lombok.AccessLevel.*;

@Entity
@DiscriminatorValue("B")
@Getter
@NoArgsConstructor(access = PUBLIC)
public class Book extends Product {

    private String author;
    private String isbn;

    public Book(String name, int price, int stockQuantity, String author, String isbn) {
        super(name, price, stockQuantity);
        this.author = author;
        this.isbn = isbn;
    }
}
