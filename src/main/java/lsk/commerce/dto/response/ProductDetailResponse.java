package lsk.commerce.dto.response;

import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.hibernate.Hibernate;

import java.util.List;

public record ProductDetailResponse(
        String name,
        String productNumber,
        Integer price,
        Integer stockQuantity,

        String dtype,

        String artist,
        String studio,

        String author,
        String isbn,

        String actor,
        String director,

        List<CategoryNameResponse> categoryNameResponseList
) {
    public static ProductDetailResponse from(Product product) {
        Object actualProduct = Hibernate.unproxy(product);
        List<CategoryNameResponse> categoryNameResponses = product.getCategoryProducts().stream()
                .map(CategoryNameResponse::from)
                .toList();

        if (actualProduct instanceof Album album) {
            return new ProductDetailResponse(album.getName(), album.getProductNumber(), album.getPrice(), album.getStockQuantity(), "A", album.getArtist(), album.getStudio(), null, null, null, null, categoryNameResponses);
        } else if (actualProduct instanceof Book book) {
            return new ProductDetailResponse(book.getName(), book.getProductNumber(), book.getPrice(), book.getStockQuantity(), "B", null, null, book.getAuthor(), book.getIsbn(), null, null, categoryNameResponses);
        } else if (actualProduct instanceof Movie movie) {
            return new ProductDetailResponse(movie.getName(), movie.getProductNumber(), movie.getPrice(), movie.getStockQuantity(), "M", null, null, null, null, movie.getActor(), movie.getDirector(), categoryNameResponses);
        } else {
            throw new IllegalArgumentException("잘못된 상품입니다. product: " + actualProduct.getClass().getName());
        }
    }
}
