package lsk.commerce.controller.form;

import io.portone.sdk.server.common.Currency;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.hibernate.validator.constraints.Range;

import static io.portone.sdk.server.common.Currency.*;

@Getter
public class ProductForm {

    @NotNull
    private String name;

    @NotNull
    @Range(min = 1, max = 100000)
    private Integer price;

    @NotNull
    @Range(min = 0, max = 1000)
    private Integer stockQuantity;

    private String dtype;

    private String artist;

    private String studio;

    private String author;

    private String isbn;

    private String director;

    private String actor;

    private final String currency = Krw.INSTANCE.getValue();

    public ProductForm(String name, Integer price, Integer stockQuantity, String dtype, String artist, String studio, String author, String isbn, String director, String actor) {
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

    public static ProductForm productChangeForm(Product product) {
        if (product instanceof Album album) {
            return new ProductForm(album.getName(), album.getPrice(), album.getStockQuantity(), "A", album.getArtist(), album.getStudio(), null, null, null, null);
        } else if (product instanceof Book book) {
            return new ProductForm(book.getName(), book.getPrice(), book.getStockQuantity(), "B", null, null, book.getAuthor(), book.getIsbn(), null, null);
        } else if (product instanceof Movie movie) {
            return new ProductForm(movie.getName(), movie.getPrice(), movie.getStockQuantity(), "M", null, null, null, null, movie.getDirector(), movie.getActor());
        } else {
            throw new IllegalArgumentException("잘못된 상품입니다. product.name: " + product.getName());
        }
    }
}
