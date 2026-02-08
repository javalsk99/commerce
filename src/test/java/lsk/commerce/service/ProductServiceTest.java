package lsk.commerce.service;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.domain.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    EntityManager em;

    @Autowired
    ProductService productService;
    @Autowired
    CategoryService categoryService;

    @Test
    void register() {
        //given
        Category category1 = createCategory1();
        Category category2 = createCategory2();
        Category category3 = createCategory3();

        Album album = createAlbum();
        Book book = createBook();
        Movie movie = createMovie();

        //when
        String albumName = productService.register(album, List.of(category1));
        String bookName = productService.register(book, List.of(category2));
        String movieName = productService.register(movie, List.of(category3));

        //then
        Product findAlbum = productService.findProductByName(albumName);
        Product findBook = productService.findProductByName(bookName);
        Product findMovie = productService.findProductByName(movieName);
        assertThat(findAlbum.getName()).isEqualTo("하얀 그리움");
        assertThat(findBook.getName()).isEqualTo("자바 ORM 표준 JPA 프로그래밍");
        assertThat(findMovie.getName()).isEqualTo("굿뉴스");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("productProvider")
    void failed_register(Product product, String reason) {
        //given
        Category category1 = createCategory1();
        Category category2 = createCategory2();
        Category category3 = createCategory3();

        //then
        assertThrows(ConstraintViolationException.class, () ->
                productService.register(product, List.of(category1, category2, category3)));
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("categoriesProvider")
    void wrongCategory_register(List<Category> categories, String reason) {
        //given
        Album album = createAlbum();

        //then
        assertThrows(IllegalArgumentException.class, () ->
                productService.register(album, categories));
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("duplicateProductProvider")
    void duplicate_register(Product product, String reason) {
        //given
        Category category1 = createCategory1();
        Category category2 = createCategory2();
        Category category3 = createCategory3();

        Album album = createAlbum();
        Book book = createBook();
        Movie movie = createMovie();

        //when
        productService.register(album, List.of(category1));
        productService.register(book, List.of(category2));
        productService.register(movie, List.of(category3));

        //then
        assertThrows(IllegalArgumentException.class, () ->
                productService.register(product, List.of(category1, category2, category3)));
    }

    @Test
    void find() {
        //given
        Category category1 = createCategory1();
        Category category2 = createCategory2();
        Category category3 = createCategory3();

        Album album = createAlbum();
        Book book = createBook();
        Movie movie = createMovie();

        String albumName = productService.register(album, List.of(category1));
        productService.register(book, List.of(category2));
        productService.register(movie, List.of(category3));

        //when
        Product findAlbum = productService.findProductByName(albumName);
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
                .extracting(p -> p.getCategoryProducts())
                .extracting(cp -> cp.size())
                .isEqualTo(1);

        assertThat(findProducts)
                .extracting("name")
                .contains("하얀 그리움", "자바 ORM 표준 JPA 프로그래밍", "굿뉴스");
    }

    @Test
    void failed_find() {
        //given
        Category category = createCategory1();
        Album album = createAlbum();
        productService.register(album, List.of(category));

        //then
        assertThrows(IllegalArgumentException.class, () ->
                productService.findProductByName("굿뉴스"));
    }

    @Test
    void update() {
        //given
        Category category = createCategory1();
        Album album = createAlbum();
        String albumName = productService.register(album, List.of(category));

        //when
        productService.updateProduct(albumName, 20000, 30);
        em.flush();
        em.clear();

        //then
        Product findAlbum = productService.findProductByName(albumName);
        assertThat(findAlbum)
                .extracting("price", "stockQuantity")
                .contains(20000, 30);
    }

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

    static Stream<Arguments> productProvider() {
        return Stream.of(
                arguments(new Album(null, 1000, 100, "artist", "studio"), "이름 null"),
                arguments(new Album("", 1000, 100, "artist", "studio"), "이름 빈 문자열"),
                arguments(new Album(" ", 1000, 100, "artist", "studio"), "이름 공백"),
                arguments(new Album("name", null, 100, "artist", "studio"), "가격 null"),
                arguments(new Album("name", 1000, null, "artist", "studio"), "재고 수량 null"),
                arguments(new Album("name", 1000, 100, null, "studio"), "가수 null"),
                arguments(new Album("name", 1000, 100, "a".repeat(51), "studio"), "가수 50자 초과"),
                arguments(new Album("name", 1000, 100, "artist", null), "기획사 null"),
                arguments(new Book("name", 1000, 100, "author", "a".repeat(9)), "isbn 10자 미만")
        );
    }

    static Stream<Arguments> categoriesProvider() {
        List<Category> emptyList = new ArrayList<>();
        List<Category> wrongCategoryList = List.of(Category.createCategory(null, "K-POP"));

        return Stream.of(
                arguments(null, "카테고리 리스트 null"),
                arguments(emptyList, "빈 카테고리 리스트"),
                arguments(wrongCategoryList, "존재하지 않는 카테고리")
        );
    }

    static Stream<Arguments> duplicateProductProvider() {
        return Stream.of(
                arguments(new Album("하얀 그리움", 10000, 30, "fromis_9", "ASND"), "이미 있는 앨범"),
                arguments(new Book("자바 ORM 표준 JPA 프로그래밍", 40000, 15, "김영한", "9788960777330"), "이미 있는 책"),
                arguments(new Movie("굿뉴스", 5000, 20, "변성현", "설경구"), "이미 있는 영화")
        );
    }
}
