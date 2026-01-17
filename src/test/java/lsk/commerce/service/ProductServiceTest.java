package lsk.commerce.service;

import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.domain.product.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    ProductService productService;

    @Test
    void register() {
        //given
        Album album = createAlbum();
        Book book = createBook();
        Movie movie = createMovie();

        //when
        Long albumId = productService.register(album);
        Long bookId = productService.register(book);
        Long movieId = productService.register(movie);

        //then
        assertThat(albumId).isEqualTo(album.getId());
        assertThat(bookId).isEqualTo(book.getId());
        assertThat(movieId).isEqualTo(movie.getId());
        assertThat(album.getArtist()).isEqualTo("fromis_9");
        assertThat(book.getAuthor()).isEqualTo("김영한");
        assertThat(movie.getActor()).isEqualTo("설경구");
    }

    @Test
    void find() {
        //given
        Album album = createAlbum();
        Book book = createBook();
        Movie movie = createMovie();

        Long albumId = productService.register(album);
        Long bookId = productService.register(book);
        Long movieId = productService.register(movie);

        //when
        Product findAlbum = productService.findProduct(albumId);
        Product findBook = productService.findProduct(bookId);
        Product findMovie = productService.findProduct(movieId);
        List<Product> findProducts = productService.findProducts();

        //then
        assertThat(findAlbum)
                .isInstanceOf(Album.class)
                .extracting("name", "artist")
                .contains("하얀 그리움", "fromis_9");

        assertThat(findProducts)
                .extracting("name")
                .contains("하얀 그리움", "자바 ORM 표준 JPA 프로그래밍", "굿뉴스");
    }

    @Test
    void delete() {
        //given
        Album album = createAlbum();
        Long albumId = productService.register(album);

        //when
        productService.deleteProduct(album);

        //then
        Product findAlbum = productService.findProduct(albumId);
        assertThat(findAlbum).isNull();
    }

    @Test
    void update() {
        //given
        Album album = createAlbum();
        Long albumId = productService.register(album);

        //when
        productService.updateProduct(albumId, 20000, 30);

        //then
        Product findAlbum = productService.findProduct(albumId);

        assertThat(findAlbum)
                .extracting("price", "stockQuantity")
                .contains(20000, 30);
    }

    private static Album createAlbum() {
        return new Album("하얀 그리움", 15000, 20, "fromis_9", "ASND");
    }

    private static Book createBook() {
        return new Book("자바 ORM 표준 JPA 프로그래밍", 43000, 10, "김영한", "9788960777330");
    }

    private static Movie createMovie() {
        return new Movie("굿뉴스", 7000, 15, "변성현", "설경구");
    }
}