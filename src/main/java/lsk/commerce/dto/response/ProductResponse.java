package lsk.commerce.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.hibernate.Hibernate;

public record ProductResponse(
        String name,
        Integer price,
        Integer stockQuantity,

        String dtype,

        String artist,
        String studio,

        String author,
        String isbn,

        String actor,
        String director
) {
    @QueryProjection
    public ProductResponse {
    }

    public static ProductResponse from(Product product) {
        Object actualProduct = Hibernate.unproxy(product);
        if (actualProduct instanceof Album album) {
            return new ProductResponse(album.getName(), album.getPrice(), album.getStockQuantity(), "A", album.getArtist(), album.getStudio(), null, null, null, null);
        } else if (actualProduct instanceof Book book) {
            return new ProductResponse(book.getName(), book.getPrice(), book.getStockQuantity(), "B", null, null, book.getAuthor(), book.getIsbn(), null, null);
        } else if (actualProduct instanceof Movie movie) {
            return new ProductResponse(movie.getName(), movie.getPrice(), movie.getStockQuantity(), "M", null, null, null, null, movie.getActor(), movie.getDirector());
        } else {
            throw new IllegalArgumentException("잘못된 상품입니다. product: " + actualProduct.getClass().getName());
        }
    }
}
