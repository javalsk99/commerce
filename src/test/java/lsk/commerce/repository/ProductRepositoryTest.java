package lsk.commerce.repository;

import jakarta.validation.ConstraintViolationException;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductRepository.class)
class ProductRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProductRepository productRepository;

    Album album;
    Book book;
    Movie movie;
    String productNumber1;
    String productNumber2;

    @BeforeEach
    void beforeEach() {
        album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build();
        book = Book.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(7).author("김영한").isbn("9788960777330").build();
        movie = Movie.builder().name("범죄도시").price(15000).stockQuantity(5).actor("마동석").director("강윤성").build();
        productNumber1 = album.getProductNumber();
        productNumber2 = book.getProductNumber();
    }

    @Nested
    class Save {

        @Nested
        class SuccessCase {

            @Test
            void album() {
                System.out.println("================= WHEN START =================");

                //when
                productRepository.save(album);
                em.flush();
                Long albumId = album.getId();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Product findProduct = em.find(Product.class, albumId);
                thenSoftly(softly -> {
                    softly.then(findProduct)
                            .isInstanceOf(Album.class)
                            .extracting("name", "nameInitial", "price", "stockQuantity", "artist", "artistInitial", "studio", "studioInitial")
                            .containsExactly("BANG BANG", "BANG BANG", 15000, 10, "IVE", "IVE", "STARSHIP", "STARSHIP");
                    softly.then(findProduct.getCategoryProducts()).isEmpty();
                });
            }

            @Test
            void book() {
                System.out.println("================= WHEN START =================");

                //when
                productRepository.save(book);
                em.flush();
                Long bookId = book.getId();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Product findProduct = em.find(Product.class, bookId);
                thenSoftly(softly -> {
                    softly.then(findProduct)
                            .isInstanceOf(Book.class)
                            .extracting("name", "nameInitial", "price", "stockQuantity", "author", "authorInitial", "isbn")
                            .containsExactly("자바 ORM 표준 JPA 프로그래밍", "ㅈㅂ ORM ㅍㅈ JPA ㅍㄹㄱㄹㅁ", 15000, 7, "김영한", "ㄱㅇㅎ", "9788960777330");
                    softly.then(findProduct.getCategoryProducts()).isEmpty();
                });
            }

            @Test
            void movie() {
                System.out.println("================= WHEN START =================");

                //when
                productRepository.save(movie);
                em.flush();
                Long movieId = movie.getId();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Product findProduct = em.find(Product.class, movieId);
                thenSoftly(softly -> {
                    softly.then(findProduct)
                            .isInstanceOf(Movie.class)
                            .extracting("name", "nameInitial", "price", "stockQuantity", "actor", "actorInitial", "director", "directorInitial")
                            .containsExactly("범죄도시", "ㅂㅈㄷㅅ", 15000, 5, "마동석", "ㅁㄷㅅ", "강윤성", "ㄱㅇㅅ");
                    softly.then(findProduct.getCategoryProducts()).isEmpty();
                });
            }

            @ParameterizedTest
            @MethodSource("albumProvider")
            void album_UniqueKeysNotExist(Product notDuplicateAlbum) {
                em.persistAndFlush(album);
                Long albumId1 = album.getId();
                em.clear();

                System.out.println("================= WHEN START =================");

                //when
                productRepository.save(notDuplicateAlbum);
                em.flush();
                Long albumId2 = notDuplicateAlbum.getId();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                then(albumId1).isNotEqualTo(albumId2);
            }

            @ParameterizedTest
            @MethodSource("bookProvider")
            void book_UniqueKeysNotExist(Product notDuplicateBook) {
                em.persistAndFlush(book);
                Long bookId1 = book.getId();
                em.clear();

                System.out.println("================= WHEN START =================");

                //when
                productRepository.save(notDuplicateBook);
                em.flush();
                Long bookId2 = notDuplicateBook.getId();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                then(bookId1).isNotEqualTo(bookId2);
            }

            @ParameterizedTest
            @MethodSource("movieProvider")
            void movie_UniqueKeysNotExist(Product notDuplicateMovie) {
                em.persistAndFlush(movie);
                Long movieId1 = movie.getId();
                em.clear();

                System.out.println("================= WHEN START =================");

                //when
                productRepository.save(notDuplicateMovie);
                em.flush();
                Long movieId2 = notDuplicateMovie.getId();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                then(movieId1).isNotEqualTo(movieId2);
            }

            static Stream<Arguments> albumProvider() {
                return Stream.of(
                        argumentSet("이름만 다른 앨범", Album.builder().name("BLACKHOLE").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build()),
                        argumentSet("가수만 다른 앨범", Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("aespa").studio("STARSHIP").build()),
                        argumentSet("기획사만 다른 앨범", Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("SM").build())
                );
            }

            static Stream<Arguments> bookProvider() {
                return Stream.of(
                        argumentSet("이름만 다른 책", Book.builder().name("면접을 위한 CS 전공지식 노트").price(15000).stockQuantity(7).author("김영한").isbn("9788960777330").build()),
                        argumentSet("작가만 다른 앨범", Book.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(7).author("주홍철").isbn("9788960777330").build()),
                        argumentSet("isbn만 다른 앨범", Book.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(7).author("김영한").isbn("9791165219529").build())
                );
            }

            static Stream<Arguments> movieProvider() {
                return Stream.of(
                        argumentSet("이름만 다른 영화", Movie.builder().name("범죄도시2").price(15000).stockQuantity(5).actor("마동석").director("강윤성").build()),
                        argumentSet("배우만 다른 영화", Movie.builder().name("범죄도시").price(15000).stockQuantity(5).actor("박지환").director("강윤성").build()),
                        argumentSet("감독만 다른 영화", Movie.builder().name("범죄도시").price(15000).stockQuantity(5).actor("마동석").director("이상용").build())
                );
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("nullFieldsProductProvider")
            void nullFields(Product product, String message) {
                //when & then
                thenThrownBy(() -> productRepository.save(product))
                        .isInstanceOf(ConstraintViolationException.class)
                        .hasMessageContaining(message);
            }

            @ParameterizedTest
            @MethodSource("wrongPriceProductProvider")
            void wrongPrice(Product product) {
                //when & then
                thenThrownBy(() -> productRepository.save(product))
                        .isInstanceOf(ConstraintViolationException.class)
                        .hasMessageContaining("100 이상이어야 합니다");
            }

            @Test
            void album_UniqueKeysAlreadyExist() {
                em.persistAndFlush(album);
                em.clear();

                Album duplicateAlbum = Album.builder().name("BANG BANG").price(12000).stockQuantity(20).artist("IVE").studio("STARSHIP").build();

                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> productRepository.save(duplicateAlbum))
                        .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class)
                        .hasMessageContaining("Duplicate entry")
                        .hasMessageContaining("UniqueAlbum");

                System.out.println("================= WHEN END ===================");
            }

            @Test
            void book_UniqueKeysAlreadyExist() {
                em.persistAndFlush(book);
                em.clear();

                Book duplicateBook = Book.builder().name("자바 ORM 표준 JPA 프로그래밍").price(20000).stockQuantity(5).author("김영한").isbn("9788960777330").build();

                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> productRepository.save(duplicateBook))
                        .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class)
                        .hasMessageContaining("Duplicate entry")
                        .hasMessageContaining("UniqueBook");

                System.out.println("================= WHEN END ===================");
            }

            @Test
            void movie_UniqueKeysAlreadyExist() {
                em.persistAndFlush(movie);
                em.clear();

                Movie duplicateMovie = Movie.builder().name("범죄도시").price(17000).stockQuantity(6).actor("마동석").director("강윤성").build();

                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> productRepository.save(duplicateMovie))
                        .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class)
                        .hasMessageContaining("Duplicate entry")
                        .hasMessageContaining("UniqueMovie");

                System.out.println("================= WHEN END ===================");
            }

            static Stream<Arguments> nullFieldsProductProvider() {
                return Stream.of(
                        argumentSet("이름 null", Album.builder().price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build(), "공백일 수 없습니다"),
                        argumentSet("가격 null", Album.builder().name("BANG BANG").stockQuantity(10).artist("IVE").studio("STARSHIP").build(), "널이어서는 안됩니다"),
                        argumentSet("재고 수량 null", Album.builder().name("유저A").price(15000).artist("IVE").studio("STARSHIP").build(), "널이어서는 안됩니다"),
                        argumentSet("artist null", Album.builder().name("유저A").price(15000).stockQuantity(10).studio("STARSHIP").build(), "공백일 수 없습니다"),
                        argumentSet("studio null", Album.builder().name("유저A").price(15000).stockQuantity(10).artist("IVE").build(), "공백일 수 없습니다")
                );
            }

            static Stream<Arguments> wrongPriceProductProvider() {
                return Stream.of(
                        argumentSet("가격 음수", Album.builder().name("BANG BANG").price(-1000).stockQuantity(10).artist("IVE").studio("STARSHIP").build(), ""),
                        argumentSet("가격 0원", Album.builder().name("BANG BANG").price(0).stockQuantity(10).artist("IVE").studio("STARSHIP").build(), ""),
                        argumentSet("가격 100원 미만", Album.builder().name("BANG BANG").price(99).stockQuantity(10).artist("IVE").studio("STARSHIP").build(), "")
                );
            }
        }
    }

    private abstract class Setup {

        Long categoryId;
        Long albumId;

        @BeforeEach
        void beforeEach() {
            Category category1 = Category.createCategory(null, "가요");
            Category category2 = Category.createCategory(category1, "댄스");
            Category category3 = Category.createCategory(category1, "발라드");
            em.persist(category1);
            categoryId = em.persistAndGetId(category2, Long.class);
            em.persist(category3);

            albumId = em.persistAndGetId(album, Long.class);
            em.persist(book);
            em.persist(movie);
            album.connectCategory(category2);

            em.flush();
            em.clear();
        }
    }

    @Nested
    class Find extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void byName() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Product> findProduct = productRepository.findByNumber(productNumber1);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findProduct).isPresent();
                    softly.then(Hibernate.isInitialized(findProduct.get().getCategoryProducts())).isFalse();
                    softly.then(Hibernate.isInitialized(findProduct.get().getCategoryProducts().getFirst().getCategory())).isFalse();
                    softly.then(Hibernate.isInitialized(findProduct.get().getCategoryProducts().getFirst().getProduct())).isTrue();
                    softly.then(findProduct.get().getCategoryProducts())
                            .extracting("category.name")
                            .containsExactlyInAnyOrder("가요", "댄스");
                });
            }

            @Test
            void withCategoryProduct() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Product> findProduct = productRepository.findWithCategoryProduct(productNumber1);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findProduct).isPresent();
                    softly.then(Hibernate.isInitialized(findProduct.get().getCategoryProducts())).isTrue();
                    softly.then(Hibernate.isInitialized(findProduct.get().getCategoryProducts().getFirst().getCategory())).isFalse();
                    softly.then(Hibernate.isInitialized(findProduct.get().getCategoryProducts().getFirst().getProduct())).isTrue();
                    softly.then(findProduct.get().getCategoryProducts())
                            .extracting("category.name")
                            .containsExactlyInAnyOrder("가요", "댄스");
                });
            }

            @Test
            void withCategoryProductCategory() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Product> findProduct = productRepository.findWithCategoryProductCategory(productNumber1);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findProduct).isPresent();
                    softly.then(Hibernate.isInitialized(findProduct.get().getCategoryProducts())).isTrue();
                    softly.then(Hibernate.isInitialized(findProduct.get().getCategoryProducts().getFirst().getCategory())).isTrue();
                    softly.then(Hibernate.isInitialized(findProduct.get().getCategoryProducts().getFirst().getProduct())).isTrue();
                    softly.then(findProduct.get().getCategoryProducts())
                            .extracting("category.name")
                            .containsExactlyInAnyOrder("가요", "댄스");
                });
            }

            @Test
            void withCategoryProduct_ShouldReturnProduct_WhenCategoryProductsIsEmpty() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Product> findProduct = productRepository.findWithCategoryProduct(productNumber2);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(findProduct).isPresent();
                    softly.then(Hibernate.isInitialized(findProduct.get().getCategoryProducts())).isTrue();
                    softly.then(findProduct.get().getCategoryProducts()).isEmpty();
                });
            }
        }
    }

    @Nested
    class Delete extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                Product findAlbum = em.find(Product.class, albumId);
                Long categoryProductId = findAlbum.getCategoryProducts().getFirst().getId();

                System.out.println("================= WHEN START =================");

                //when
                productRepository.delete(findAlbum);
                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Product deletedAlbum = em.find(Product.class, albumId);
                CategoryProduct deletedCategoryProduct = em.find(CategoryProduct.class, categoryProductId);
                Category deletedCategory = em.find(Category.class, categoryId);
                thenSoftly(softly -> {
                    softly.then(deletedAlbum).isNull();
                    softly.then(deletedCategoryProduct).isNull();
                    softly.then(deletedCategory.getCategoryProducts()).isEmpty();
                });
            }
        }
    }

    @Nested
    class Exists extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void album_ShouldReturnTrue_WhenAllUniqueKeysMatch() {
                System.out.println("================= WHEN START =================");

                //when
                boolean result = productRepository.existsAlbum("BANG BANG", "IVE", "STARSHIP");

                System.out.println("================= WHEN END ===================");

                //then
                then(result).isTrue();
            }

            @Test
            void book_ShouldReturnTrue_WhenAllUniqueKeysMatch() {
                System.out.println("================= WHEN START =================");

                //when
                boolean result = productRepository.existsBook("자바 ORM 표준 JPA 프로그래밍", "김영한", "9788960777330");

                System.out.println("================= WHEN END ===================");

                //then
                then(result).isTrue();
            }

            @Test
            void movie_ShouldReturnTrue_WhenAllUniqueKeysMatch() {
                System.out.println("================= WHEN START =================");

                //when
                boolean result = productRepository.existsMovie("범죄도시", "마동석", "강윤성");

                System.out.println("================= WHEN END ===================");

                //then
                then(result).isTrue();
            }

            @ParameterizedTest
            @CsvSource({
                    "BLACKHOLE, IVE, STARSHIP",
                    "BANG BANG, aespa, STARSHIP",
                    "BANG BANG, IVE, SM"
            })
            void album_ShouldReturnFalse_WhenUniqueKeysMismatch(String name, String artist, String studio) {
                System.out.println("================= WHEN START =================");

                //when
                boolean result = productRepository.existsAlbum(name, artist, studio);

                System.out.println("================= WHEN END ===================");

                //then
                then(result).isFalse();
            }

            @ParameterizedTest
            @CsvSource({
                    "면접을 위한 CS 전공지식 노트, 김영한, 9788960777330",
                    "자바 ORM 표준 JPA 프로그래밍, 주홍철, 9788960777330",
                    "자바 ORM 표준 JPA 프로그래밍, 김영한, 9791165219529"
            })

            void book_ShouldReturnFalse_WhenUniqueKeysMismatch(String name, String author, String isbn) {
                System.out.println("================= WHEN START =================");

                //when
                boolean result = productRepository.existsBook(name, author, isbn);

                System.out.println("================= WHEN END ===================");

                //then
                then(result).isFalse();
            }

            @ParameterizedTest
            @CsvSource({
                    "범죄도시2, 마동석, 강윤성",
                    "범죄도시, 박지환, 강윤성",
                    "범죄도시, 마동석, 이상용"
            })
            void movie_ShouldReturnFalse_WhenUniqueKeysMismatch(String name, String actor, String director) {
                System.out.println("================= WHEN START =================");

                //when
                boolean result = productRepository.existsMovie(name, actor, director);

                System.out.println("================= WHEN END ===================");

                //then
                then(result).isFalse();
            }
        }
    }
}