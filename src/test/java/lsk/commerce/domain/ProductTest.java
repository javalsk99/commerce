package lsk.commerce.domain;

import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenNoException;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

class ProductTest {

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                Album album = createAlbum();
                Book book = createBook();
                Movie movie = createMovie();

                //then
                thenSoftly(softly -> {
                    softly.then(album)
                            .extracting("name", "artist", "studio")
                            .contains("BANG BANG", "IVE", "STARSHIP");
                    softly.then(book)
                            .extracting("name", "author", "isbn")
                            .contains("자바 ORM 표준 JPA 프로그래밍", "김영한", "9788960777330");
                    softly.then(movie)
                            .extracting("name", "actor", "director")
                            .contains("범죄도시", "마동석", "강윤성");
                });
            }

            private Album createAlbum() {
                return Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
            }

            private Book createBook() {
                return Book.builder()
                        .name("자바 ORM 표준 JPA 프로그래밍")
                        .price(15000)
                        .stockQuantity(7)
                        .author("김영한")
                        .isbn("9788960777330")
                        .build();
            }

            private Movie createMovie() {
                return Movie.builder()
                        .name("범죄도시")
                        .price(15000)
                        .stockQuantity(5)
                        .actor("마동석")
                        .director("강윤성")
                        .build();
            }
        }
    }

    @Nested
    class AddStock {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = Album.builder()
                        .stockQuantity(3)
                        .build();

                //when
                album.addStock(4);

                //then
                then(album.getStockQuantity()).isEqualTo(7);
            }
        }

        @Nested
        class FailureCase {

            @Test
            void stockNull() {
                //given
                Album album = Album.builder()
                        .stockQuantity(3)
                        .build();

                //when & then
                thenThrownBy(() -> album.addStock(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("재고가 추가될 수량이 없습니다");

                //then
                then(album.getStockQuantity()).isEqualTo(3);
            }
        }
    }

    @Nested
    class RemoveStock {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = Album.builder()
                        .stockQuantity(3)
                        .build();

                //when
                album.removeStock(2);

                //then
                then(album.getStockQuantity()).isEqualTo(1);
            }
        }

        @Nested
        class FailureCase {

            @Test
            void stockNull() {
                //given
                Album album = Album.builder()
                        .stockQuantity(3)
                        .build();

                //when & then
                thenThrownBy(() -> album.removeStock(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("재고가 감소될 수량이 없습니다");

                //then
                then(album.getStockQuantity()).isEqualTo(3);
            }

            @Test
            void exceed() {
                //given
                Album album = Album.builder()
                        .stockQuantity(3)
                        .build();

                //when & then
                thenThrownBy(() -> album.removeStock(4))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("재고가 부족합니다. productNumber: " + album.getProductNumber());

                //then
                then(album.getStockQuantity()).isEqualTo(3);
            }
        }
    }

    @Nested
    class ChangeStock {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = Album.builder()
                        .stockQuantity(3)
                        .build();

                //when
                album.changeStock(2, 3);

                //then
                then(album.getStockQuantity()).isEqualTo(2);
            }
        }

        @Nested
        class FailureCase {

            @Test
            void stockNull() {
                //given
                Album album = Album.builder()
                        .stockQuantity(3)
                        .build();

                //when & then
                thenSoftly(softly -> {
                    softly.thenThrownBy(() -> album.changeStock(null, 3))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("재고가 추가될 수량이 없습니다");
                    softly.thenThrownBy(() -> album.changeStock(2, null))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("재고가 감소될 수량이 없습니다");
                });

                //then
                then(album.getStockQuantity()).isEqualTo(3);
            }

            @Test
            void exceed() {
                //given
                Album album = Album.builder()
                        .stockQuantity(3)
                        .build();

                //when & then
                thenThrownBy(() -> album.changeStock(1, 5))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("재고가 부족합니다. productNumber: " + album.getProductNumber());

                //then
                then(album.getStockQuantity()).isEqualTo(3);
            }
        }
    }

    @Nested
    class Change {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = Album.builder()
                        .price(15000)
                        .stockQuantity(10)
                        .build();

                //when
                album.changePriceAndStock(12000, 20);

                //then
                then(album)
                        .extracting("price", "stockQuantity")
                        .contains(12000, 20);
            }

            @Test
            void priceNull() {
                //given
                Album album = Album.builder()
                        .price(15000)
                        .stockQuantity(10)
                        .build();

                //when
                album.changePriceAndStock(null, 20);

                //then
                then(album)
                        .extracting("price", "stockQuantity")
                        .contains(15000, 20);
            }

            @Test
            void stockNull() {
                //given
                Album album = Album.builder()
                        .price(15000)
                        .stockQuantity(10)
                        .build();

                //when
                album.changePriceAndStock(12000, null);

                //then
                then(album)
                        .extracting("price", "stockQuantity")
                        .contains(12000, 10);
            }
        }
    }

    abstract class CategorySetup {

        Category category1;
        Category category2;
        Category category3;

        @BeforeEach
        void beforeEach() {
            category1 = Category.createCategory(null, "가요");
            category2 = Category.createCategory(category1, "댄스");
            category3 = Category.createCategory(category1, "발라드");

            ReflectionTestUtils.setField(category1, "id", 1L);
            ReflectionTestUtils.setField(category2, "id", 2L);
        }
    }

    @Nested
    class ConnectCategory extends CategorySetup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = Album.builder()
                        .name("BANG BANG")
                        .build();

                //when
                album.connectCategory(category2);

                //then
                then(album.getCategoryProducts())
                        .hasSize(2)
                        .extracting("category.name", "product.name")
                        .containsExactlyInAnyOrder(tuple("가요", "BANG BANG"), tuple("댄스", "BANG BANG"));
            }
        }

        @Nested
        class FailureCase {

            @Test
            void categoryIdIsNull() {
                //given
                Album album = new Album();

                //when & then
                thenThrownBy(() -> album.connectCategory(category3))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("식별자가 없는 잘못된 카테고리입니다");

                //then
                then(album.getCategoryProducts()).isEmpty();
            }
        }
    }

    @Nested
    class ConnectCategories extends CategorySetup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = Album.builder()
                        .name("BANG BANG")
                        .build();

                //when
                album.connectCategories(List.of(category2));

                //then
                thenSoftly(softly -> {
                    softly.then(album.getCategoryProducts())
                            .hasSize(2)
                            .extracting("category.name")
                            .containsExactlyInAnyOrder("가요", "댄스");
                    softly.then(category1.getCategoryProducts())
                            .isNotEmpty()
                            .isNotEqualTo(category2.getCategoryProducts())
                            .extracting("product.name")
                            .containsExactly("BANG BANG");
                    softly.then(category2.getCategoryProducts())
                            .isNotEmpty()
                            .extracting("product.name")
                            .containsExactly("BANG BANG");
                });
            }

            @Test
            void ignoreDuplicateParent() {
                //given
                Album album = Album.builder()
                        .name("BANG BANG")
                        .build();

                //when
                album.connectCategories(List.of(category1, category2));

                //then
                thenSoftly(softly -> {
                    softly.then(album.getCategoryProducts())
                            .hasSize(2)
                            .extracting("category.name")
                            .containsExactlyInAnyOrder("가요", "댄스");
                    softly.then(category1.getCategoryProducts())
                            .isNotEmpty()
                            .isNotEqualTo(category2.getCategoryProducts())
                            .extracting("product.name")
                            .containsExactly("BANG BANG");
                    softly.then(category2.getCategoryProducts())
                            .isNotEmpty()
                            .extracting("product.name")
                            .containsExactly("BANG BANG");
                });
            }
        }
    }

    @Nested
    class RemoveCategoryProductsFormCategory extends CategorySetup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = new Album();

                album.connectCategories(List.of(category2));

                //when
                album.removeCategoryProductsFormCategory();

                //then
                thenSoftly(softly -> {
                    softly.then(category1.getCategoryProducts()).isEmpty();
                    softly.then(category2.getCategoryProducts()).isEmpty();
                    softly.then(album.getCategoryProducts()).hasSize(2);
                });
            }

            @Test
            void idempotency() {
                //given
                Album album = new Album();

                album.connectCategories(List.of(category2));

                //when 첫 번째 호출
                album.removeCategoryProductsFormCategory();

                //then
                thenSoftly(softly -> {
                    softly.then(category1.getCategoryProducts()).isEmpty();
                    softly.then(category2.getCategoryProducts()).isEmpty();
                    softly.then(album.getCategoryProducts()).hasSize(2);
                });

                //when & then 두 번째 호출
                thenNoException().isThrownBy(() -> album.removeCategoryProductsFormCategory());

                //then
                thenSoftly(softly -> {
                    softly.then(category1.getCategoryProducts()).isEmpty();
                    softly.then(category2.getCategoryProducts()).isEmpty();
                    softly.then(album.getCategoryProducts()).hasSize(2);
                });
            }
        }
    }

    @Nested
    class RemoveCategoryProduct extends CategorySetup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = new Album();

                album.connectCategory(category1);

                //when
                album.removeCategoryProduct(category1);

                //then
                thenSoftly(softly -> {
                    softly.then(album.getCategoryProducts()).isEmpty();
                    softly.then(category1.getCategoryProducts()).isEmpty();
                });
            }
        }

        @Nested
        class FailureCase extends CategorySetup {

            @Test
            void categoryProductsIsEmpty() {
                //given
                Album album = new Album();

                //when & then
                thenThrownBy(() -> album.removeCategoryProduct(category1))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("상품과 연결된 카테고리가 없습니다");
            }

            @Test
            void notLinked() {
                //given
                Album album = new Album();

                album.connectCategory(category1);

                //when & then
                thenThrownBy(() -> album.removeCategoryProduct(category2))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("상품이 해당 카테고리에 없습니다");

                //then
                then(album.getCategoryProducts())
                        .isNotEmpty()
                        .extracting("category.name")
                        .containsExactly("가요");
            }
        }
    }
}