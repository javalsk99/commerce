package lsk.commerce.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lsk.commerce.domain.Product;
import lsk.commerce.util.InitialExtractor;

import static lombok.AccessLevel.PUBLIC;

@Entity
@DiscriminatorValue("B")
@Getter
@NoArgsConstructor(access = PUBLIC)
public class Book extends Product {

    @NotBlank @Size(max = 50)
    @Column(length = 50)
    private String author;

    @Column(length = 50)
    private String authorInitial;

    @NotBlank @Size(min = 10, max = 13)
    @Column(length = 13)
    private String isbn;

    @Builder
    public Book(String name, Integer price, Integer stockQuantity, String author, String isbn) {
        super(name, price, stockQuantity);
        this.author = author;
        this.isbn = isbn;
    }

    @Override
    protected void preHandler() {
        super.preHandler();
        this.authorInitial = InitialExtractor.extract(this.author);
    }
}
