package lsk.commerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.Range;

@Getter
public class ProductRequest {

    @NotNull
    private String name;

    @NotNull
    @Range(min = 1, max = 100000)
    private Integer price;

    @NotNull
    @Range(min = 0, max = 1000)
    private Integer stockQuantity;

    @NotNull
    private String dtype;

    private String artist;

    private String studio;

    private String author;

    private String isbn;

    private String director;

    private String actor;

    public ProductRequest(String name, Integer price, Integer stockQuantity, String dtype, String artist, String studio, String author, String isbn, String director, String actor) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.dtype = dtype;
        this.artist = artist;
        this.studio = studio;
        this.author = author;
        this.isbn = isbn;
        this.director = director;
        this.actor = actor;
    }
}
