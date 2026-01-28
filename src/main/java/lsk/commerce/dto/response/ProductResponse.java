package lsk.commerce.dto.response;

import lombok.Getter;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.hibernate.Hibernate;

import static io.portone.sdk.server.common.Currency.*;

@Getter
public class ProductResponse {

    private String name;

    private int price;

    private int stockQuantity;

    private String dtype;

    private String artist;

    private String studio;

    private String author;

    private String isbn;

    private String director;

    private String actor;

    private final String currency = Krw.INSTANCE.getValue();

    public ProductResponse(String name, int price, int stockQuantity, String dtype, String artist, String studio, String author, String isbn, String director, String actor) {
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

    public static ProductResponse productChangeDto(Product product) {
        Object unproxiedProduct = Hibernate.unproxy(product);
        if (unproxiedProduct instanceof Album album) {
            return new ProductResponse(album.getName(), album.getPrice(), album.getStockQuantity(), "A", album.getArtist(), album.getStudio(), null, null, null, null);
        } else if (unproxiedProduct instanceof Book book) {
            return new ProductResponse(book.getName(), book.getPrice(), book.getStockQuantity(), "B", null, null, book.getAuthor(), book.getIsbn(), null, null);
        } else if (unproxiedProduct instanceof Movie movie) {
            return new ProductResponse(movie.getName(), movie.getPrice(), movie.getStockQuantity(), "M", null, null, null, null, movie.getDirector(), movie.getActor());
        } else {
            throw new IllegalArgumentException("잘못된 상품입니다. product: " + unproxiedProduct.getClass().getName());
        }
    }
}
