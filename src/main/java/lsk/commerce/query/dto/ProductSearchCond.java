package lsk.commerce.query.dto;

import lombok.Getter;
import lombok.Setter;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;

@Getter @Setter
public class ProductSearchCond {

    private String name;
    private Integer minPrice;
    private Integer maxPrice;

    private String dtype;

    private String artist;
    private String studio;

    private String author;
    private String isbn;

    private String actor;
    private String director;

    public ProductSearchCond(String name, Integer minPrice, Integer maxPrice, String dtype, String artist,
                             String studio, String author, String isbn, String actor, String director) {
        this.name = name;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.dtype = dtype;
        this.artist = artist;
        this.studio = studio;
        this.author = author;
        this.isbn = isbn;
        this.actor = actor;
        this.director = director;
    }
}
