package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.domain.Product;
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
    @Autowired
    CategoryService categoryService;

    @Test
    void register() {
        //given
        Album album = createAlbum();
        Book book = createBook();
        Movie movie = createMovie();

        Category category1 = createCategory1();
        Category category2 = createCategory2();
        Category category3 = createCategory3();

        //when
        Long albumId = productService.register(album, List.of(category1));
        Long bookId = productService.register(book, List.of(category2));
        Long movieId = productService.register(movie, List.of(category3));

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

        Category category1 = createCategory1();
        Category category2 = createCategory2();
        Category category3 = createCategory3();

        Long albumId = productService.register(album, List.of(category1));
        productService.register(book, List.of(category2));
        productService.register(movie, List.of(category3));

        //when
        Product findAlbum = productService.findProduct(albumId);
        Product findBook = productService.findProductByName(book.getName());
        Product findMovie = productService.findProductWithCategoryProduct(movie.getName());
        List<Product> findProducts = productService.findProducts();

        //then
        assertThat(findAlbum)
                .isInstanceOf(Album.class)
                .extracting("name", "artist")
                .contains("하얀 그리움", "fromis_9");

        assertThat(findBook)
                .isInstanceOf(Book.class)
                .extracting("name", "author")
                .contains("자바 ORM 표준 JPA 프로그래밍", "김영한");

        assertThat(findMovie)
                .isInstanceOf(Movie.class)
                .extracting("name", "actor")
                .contains("굿뉴스", "설경구");

        assertThat(findProducts)
                .extracting("name")
                .contains("하얀 그리움", "자바 ORM 표준 JPA 프로그래밍", "굿뉴스");
    }

    @Test
    void update() {
        //given
        Album album = createAlbum();
        Category category1 = createCategory1();
        Long albumId = productService.register(album, List.of(category1));

        //when
        productService.updateProduct(albumId, 20000, 30);
        Product findAlbum = productService.findProduct(albumId);

        //then
        assertThat(findAlbum)
                .extracting("price", "stockQuantity")
                .contains(20000, 30);
    }

/*
    @Test
    void register_product_to_category() {
        //given
        Category parentCategory = createParentCategory("컴퓨터/IT");
        Category childCategory1 = createChildCategory(parentCategory, "프로그래밍 언어");
        Category childCategory2 = createChildCategory(childCategory1, "Java");

        Book book = createBook();

        //when
        Long bookId = productService.register(book, childCategory2);
        Product findBook = productService.findProduct(bookId);

        //then
        assertThat(findBook.getCategoryProducts())
                .extracting(CategoryProduct::getCategory)
                .contains(parentCategory, childCategory1, childCategory2);
    }
*/

/*
    @Test
    void connect_category() {
        //given
        Album album = createAlbum();
        Long albumId = productService.register(album);
        Product findAlbum = productService.findProduct(albumId);

        Category parentCategory = createParentCategory("Dance");
        Category childCategory = createChildCategory(parentCategory, "Girl Group");

        //when
        findAlbum.addCategoryProduct(childCategory);

        //then
        assertThat(findAlbum.getCategoryProducts().size()).isEqualTo(2);
        assertThat(findAlbum.getCategoryProducts())
                .extracting(CategoryProduct::getCategory)
                .contains(parentCategory, childCategory);
    }
*/

/*
    @Test
    void delete() {
        //given
        Category parentCategory = createParentCategory("Dance");
        Category childCategory = createChildCategory(parentCategory, "Girl Group");

        Album album = createAlbum();
        Long albumId = productService.register(album, childCategory);

        //when
        productService.deleteProduct(album);
        Product findAlbum = productService.findProduct(albumId);

        //then
        assertThat(findAlbum).isNull();
        assertThat(parentCategory.getCategoryProducts().size()).isEqualTo(0);
        assertThat(childCategory.getCategoryProducts().size()).isEqualTo(0);
    }
*/

    private Album createAlbum() {
        return new Album("하얀 그리움", 15000, 20, "fromis_9", "ASND");
    }

    private Book createBook() {
        return new Book("자바 ORM 표준 JPA 프로그래밍", 43000, 10, "김영한", "9788960777330");
    }

    private Movie createMovie() {
        return new Movie("굿뉴스", 7000, 15, "변성현", "설경구");
    }

    private Category createCategory1() {
        return categoryService.findCategoryByName(categoryService.create("K-POP", null));
    }

    private Category createCategory2() {
        return categoryService.findCategoryByName(categoryService.create("IT/컴퓨터", null));
    }

    private Category createCategory3() {
        return categoryService.findCategoryByName(categoryService.create("Comedy", null));
    }
}
