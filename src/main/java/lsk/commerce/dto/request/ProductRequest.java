package lsk.commerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class ProductRequest {

    @NotBlank @Size(max = 50)
    private String name;

    @NotNull @Min(100)
    private Integer price;

    @NotNull @Min(0)
    private Integer stockQuantity;

    @NotBlank @Size(min = 1, max = 1)
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
