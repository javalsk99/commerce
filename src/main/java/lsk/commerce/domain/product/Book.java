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
@DiscriminatorValue("B")
@Getter
@NoArgsConstructor(access = PUBLIC)
public class Book extends Product {

    @NotBlank
    @Column(length = 50)
    private String author;

    @NotBlank
    @Column(length = 20)
    private String isbn;

    public Book(String name, int price, int stockQuantity, String author, String isbn) {
        super(name, price, stockQuantity);
        this.author = author;
        this.isbn = isbn;
    }
}
