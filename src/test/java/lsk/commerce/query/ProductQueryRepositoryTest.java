package lsk.commerce.query;

import lsk.commerce.config.QuerydslConfig;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.query.dto.ProductSearchCond;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        ProductQueryRepository.class,
        QuerydslConfig.class
})
class ProductQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProductQueryRepository productQueryRepository;

    @BeforeEach
    void beforeEach() {
        initCreateProductsAndCategories();
        em.flush();
        em.clear();
    }

    @Nested
    class Search {

        @Nested
        class SuccessCase {

            @Test
            void shouldFindAll_WhenCondIsEmpty() {
                //given
                ProductSearchCond cond = ProductSearchCond.builder().build();

                System.out.println("================= WHEN START =================");

                //when
                List<ProductResponse> productResponseList = productQueryRepository.search(cond);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(productResponseList)
                            .filteredOn(p -> "A".equals(p.dtype()))
                            .hasSize(6)
                            .extracting("name", "artist", "studio")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", "IVE", "STARSHIP"),
                                    tuple("Blue Valentine", "NMIXX", "JYP"),
                                    tuple("404", "KiiiKiii", "STARSHIP"),
                                    tuple("타임 캡슐", "다비치", "씨에이엠위더스"),
                                    tuple("너의 모든 순간", "성시경", "에스케이재원"),
                                    tuple("천상연", "이창섭", "판타지오")
                            );
                    softly.then(productResponseList)
                            .filteredOn(p -> "B".equals(p.dtype()))
                            .hasSize(3)
                            .extracting("name", "author", "isbn")
                            .containsExactlyInAnyOrder(
                                    tuple("자바 ORM 표준 JPA 프로그래밍", "김영한", "9788960777330"),
                                    tuple("면접을 위한 CS 전공지식 노트", "주홍철", "9791165219529"),
                                    tuple("Do it! 점프 투 파이썬", "박응용", "9791163034735")
                            );
                    softly.then(productResponseList)
                            .filteredOn(p -> "M".equals(p.dtype()))
                            .hasSize(4)
                            .extracting("name", "actor", "director")
                            .containsExactlyInAnyOrder(
                                    tuple("범죄도시", "마동석", "강윤성"),
                                    tuple("범죄도시2", "마동석", "이상용"),
                                    tuple("범죄도시3", "마동석", "이상용"),
                                    tuple("범죄도시4", "마동석", "허명행")
                            );
                });
            }

            @ParameterizedTest
            @MethodSource("categoryNameCondProvider")
            void shouldFilterByExactCategoryName_WhenCategoryNameIsPresent(ProductSearchCond cond, List<String> productNames) {
                assertThatContainsExactlyProductNames(cond, productNames);
            }

            @ParameterizedTest
            @MethodSource("productNameCondProvider")
            void shouldFilterByProductName_WhenProductNameIsPresent(ProductSearchCond cond, List<String> productNames) {
                assertThatContainsExactlyProductNames(cond, productNames);
            }

            @ParameterizedTest
            @MethodSource("priceRangeCondProvider")
            void shouldFilterByPriceRange_WhenPricesAreOptional(ProductSearchCond cond, List<String> productNames) {
                assertThatContainsExactlyProductNames(cond, productNames);
            }

            @ParameterizedTest
            @MethodSource("childFieldsCondProvider")
            void shouldFilterByChildFields_WhenEachChildFieldsProvided(ProductSearchCond cond, List<String> productNames) {
                assertThatContainsExactlyProductNames(cond, productNames);
            }

            @ParameterizedTest
            @MethodSource("variousFieldsCondProvider")
            void shouldFilterByMultiple_WhenVariousFieldsProvided(ProductSearchCond cond, List<String> productNames) {
                assertThatContainsExactlyProductNames(cond, productNames);
            }

            @ParameterizedTest
            @MethodSource("mismatchDtypeCondProvider")
            void shouldReturnEmpty_WhenDtypeMismatch(ProductSearchCond cond) {
                System.out.println("================= WHEN START =================");

                //when
                List<ProductResponse> productResponseList = productQueryRepository.search(cond);

                System.out.println("================= WHEN END ===================");

                //then
                then(productResponseList).isEmpty();
            }

            static Stream<Arguments> categoryNameCondProvider() {
                return Stream.of(
                        argumentSet("카테고리 이름을 가요로 검색", ProductSearchCond.builder().categoryName("가요").build(), List.of("BANG BANG", "Blue Valentine", "404")),
                        argumentSet("카테고리 이름을 ㄱㅇ으로 검색", ProductSearchCond.builder().categoryName("ㄱㅇ").build(), Collections.emptyList()),
                        argumentSet("카테고리 이름을 컴퓨터/IT로 검색", ProductSearchCond.builder().categoryName("컴퓨터/IT").build(), List.of("자바 ORM 표준 JPA 프로그래밍", "면접을 위한 CS 전공지식 노트")),
                        argumentSet("카테고리 이름을 컴퓨터로 검색", ProductSearchCond.builder().categoryName("컴퓨터").build(), Collections.emptyList()),
                        argumentSet("카테고리 이름을 국내 영화로 검색", ProductSearchCond.builder().categoryName("국내 영화").build(), List.of("범죄도시", "범죄도시2")),
                        argumentSet("카테고리 이름을 록으로 검색", ProductSearchCond.builder().categoryName("록").build(), Collections.emptyList())
                );
            }

            static Stream<Arguments> productNameCondProvider() {
                return Stream.of(
                        argumentSet("상품 이름을 BANG BANG으로 검색", ProductSearchCond.builder().productName("BANG BANG").build(), List.of("BANG BANG")),
                        argumentSet("상품 이름을 b로 검색", ProductSearchCond.builder().productName("b").build(), List.of("BANG BANG", "Blue Valentine")),
                        argumentSet("상품 이름을 ㅂ으로 검색", ProductSearchCond.builder().productName("ㅂ").build(), List.of("자바 ORM 표준 JPA 프로그래밍", "범죄도시", "범죄도시2", "범죄도시3", "범죄도시4")),
                        argumentSet("상품 이름을 ㄱㄴㄷ으로 검색", ProductSearchCond.builder().productName("ㄱㄴㄷ").build(), Collections.emptyList())
                );
            }

            static Stream<Arguments> priceRangeCondProvider() {
                return Stream.of(
                        argumentSet("가격 5000 이상", ProductSearchCond.builder().minPrice(5000).build(), List.of("BANG BANG", "Blue Valentine", "404", "타임 캡슐", "너의 모든 순간", "자바 ORM 표준 JPA 프로그래밍", "면접을 위한 CS 전공지식 노트", "범죄도시", "범죄도시2", "범죄도시3", "범죄도시4")),
                        argumentSet("가격 10000 이하", ProductSearchCond.builder().maxPrice(10000).build(), List.of("BANG BANG", "Blue Valentine", "404", "타임 캡슐", "천상연", "면접을 위한 CS 전공지식 노트", "Do it! 점프 투 파이썬", "범죄도시", "범죄도시2", "범죄도시3")),
                        argumentSet("가격 5000이상, 10000 이하", ProductSearchCond.builder().minPrice(5000).maxPrice(10000).build(), List.of("BANG BANG", "Blue Valentine", "404", "타임 캡슐", "면접을 위한 CS 전공지식 노트", "범죄도시", "범죄도시2", "범죄도시3")),
                        argumentSet("가격 10000이상, 5000 이하", ProductSearchCond.builder().minPrice(10000).maxPrice(5000).build(), Collections.emptyList())
                );
            }

            static Stream<Arguments> childFieldsCondProvider() {
                return Stream.of(
                        argumentSet("artist를 i로 검색", ProductSearchCond.builder().artist("i").build(), List.of("BANG BANG", "Blue Valentine", "404")),
                        argumentSet("artist를 i, studio를 s로 검색", ProductSearchCond.builder().artist("i").studio("s").build(), List.of("BANG BANG", "404")),
                        argumentSet("isbn을 0으로 검색", ProductSearchCond.builder().isbn("0").build(), List.of("자바 ORM 표준 JPA 프로그래밍", "Do it! 점프 투 파이썬")),
                        argumentSet("author를 ㄱ, isbn을 0으로 검색", ProductSearchCond.builder().author("ㄱ").isbn("0").build(), List.of("자바 ORM 표준 JPA 프로그래밍")),
                        argumentSet("actor를 ㅁ으로 검색", ProductSearchCond.builder().actor("ㅁ").build(), List.of("범죄도시", "범죄도시2", "범죄도시3", "범죄도시4")),
                        argumentSet("actor를 ㅁ, director를 ㄱ으로 검색", ProductSearchCond.builder().actor("ㅁ").director("ㄱ").build(), List.of("범죄도시"))
                );
            }

            static Stream<Arguments> variousFieldsCondProvider() {
                return Stream.of(
                        argumentSet("카테고리 이름을 가요, 상품 이름을 b로 검색", ProductSearchCond.builder().categoryName("가요").productName("b").build(), List.of("BANG BANG", "Blue Valentine")),
                        argumentSet("상품 이름을 b, artist를 i로 검색", ProductSearchCond.builder().productName("b").artist("i").build(), List.of("BANG BANG", "Blue Valentine")),
                        argumentSet("카테고리 이름을 컴퓨터/IT, isbn을 0으로 검색", ProductSearchCond.builder().categoryName("컴퓨터/IT").isbn("0").build(), List.of("자바 ORM 표준 JPA 프로그래밍")),
                        argumentSet("가격 8000 이하, director를 ㄱ으로 검색", ProductSearchCond.builder().maxPrice(8000).director("ㄱ").build(), List.of("범죄도시")),
                        argumentSet("카테고리 이름을 가요, 가격 8000 이상, artist를 ㅅ으로 검색", ProductSearchCond.builder().categoryName("가요").minPrice(8000).artist("ㅅ").build(), Collections.emptyList())
                );
            }

            static Stream<Arguments> mismatchDtypeCondProvider() {
                return Stream.of(
                        argumentSet("Dtype: A(artist), B(author)", ProductSearchCond.builder().artist("i").author("ㄱ").build()),
                        argumentSet("Dtype: A(artist), B(author), M(actor)", ProductSearchCond.builder().artist("i").author("ㄱ").actor("ㅁ").build()),
                        argumentSet("Dtype: A(artist), A(studio), M(director)", ProductSearchCond.builder().artist("i").studio("s").director("ㄱ").build())
                );
            }

            private void assertThatContainsExactlyProductNames(ProductSearchCond cond, List<String> productNames) {
                System.out.println("================= WHEN START =================");

                //when
                List<ProductResponse> productResponseList = productQueryRepository.search(cond);

                System.out.println("================= WHEN END ===================");

                //then
                then(productResponseList)
                        .extracting("name")
                        .containsExactlyInAnyOrderElementsOf(productNames);
            }
        }
    }

    private void initCreateProductsAndCategories() {
        Category category1 = createCategory("가요");
        Category category2 = createCategory("컴퓨터/IT");
        Category category3 = createCategory("국내 영화");
        createCategory("록");

        Album album1 = createAlbum("BANG BANG", "IVE", "STARSHIP", 7000);
        Album album2 = createAlbum("Blue Valentine", "NMIXX", "JYP", 8000);
        Album album3 = createAlbum("404", "KiiiKiii", "STARSHIP", 6000);
        createAlbum("타임 캡슐", "다비치", "씨에이엠위더스", 5000);
        createAlbum("너의 모든 순간", "성시경", "에스케이재원", 11000);
        createAlbum("천상연", "이창섭", "판타지오", 3000);
        Book book1 = createBook("자바 ORM 표준 JPA 프로그래밍", "김영한", "9788960777330", 12000);
        Book book2 = createBook("면접을 위한 CS 전공지식 노트", "주홍철", "9791165219529", 9000);
        createBook("Do it! 점프 투 파이썬", "박응용", "9791163034735", 2000);
        Movie movie1 = createMovie("범죄도시", "마동석", "강윤성", 8000);
        Movie movie2 = createMovie("범죄도시2", "마동석", "이상용", 7000);
        createMovie("범죄도시3", "마동석", "이상용", 10000);
        createMovie("범죄도시4", "마동석", "허명행", 11000);

        connectCategory1(category1, album1, album2, album3);
        connectCategory2(category2, book1, book2);
        connectCategory3(category3, movie1, movie2);
    }

    private Category createCategory(String name) {
        Category category = Category.createCategory(null, name);
        em.persist(category);
        return category;
    }

    private Album createAlbum(String name, String artist, String studio, int price) {
        Album album = Album.builder()
                .name(name)
                .price(price)
                .stockQuantity(10)
                .artist(artist)
                .studio(studio)
                .build();
        em.persist(album);
        return album;
    }

    private Book createBook(String name, String author, String isbn, int price) {
        Book book = Book.builder()
                .name(name)
                .price(price)
                .stockQuantity(7)
                .author(author)
                .isbn(isbn)
                .build();
        em.persist(book);
        return book;
    }

    private Movie createMovie(String name, String actor, String director, int price) {
        Movie movie = Movie.builder()
                .name(name)
                .price(price)
                .stockQuantity(5)
                .actor(actor)
                .director(director)
                .build();
        em.persist(movie);
        return movie;
    }

    private void connectCategory1(Category category, Album... albums) {
        for (Album album : albums) {
            album.connectCategory(category);
        }
    }

    private void connectCategory2(Category category, Book... books) {
        for (Book book : books) {
            book.connectCategory(category);
        }
    }

    private void connectCategory3(Category category, Movie... movies) {
        for (Movie movie : movies) {
            movie.connectCategory(category);
        }
    }
}