package lsk.commerce.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank
    @Pattern(regexp = "^[A-Za-z가-힣 (),]{1,50}$", message = "작가는 한글, 영문, 공백, 특수문자((),)만 사용하여 1~50자 사이로 입력해 주세요")
    @Column(length = 50)
    private String author;

    @Column(length = 50)
    private String authorInitial;

    @NotBlank
    @Pattern(regexp = "^(\\d{10}|\\d{13})$", message = "isbn은 숫자만 사용하여 10자 또는 13자로 입력해 주세요")
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
