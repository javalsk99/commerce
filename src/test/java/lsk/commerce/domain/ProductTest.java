package lsk.commerce.domain;

import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

class ProductTest {

    @Nested
    class SuccessCase {

        @Test
        void create() {
            //when
            Album album = createAlbum();
            Book book = createBook();
            Movie movie = createMovie();

            //then
            assertAll(
                    () -> assertThat(album)
                            .extracting("name", "artist", "studio")
                            .contains("BANG BANG", "IVE", "STARSHIP"),
                    () -> assertThat(book)
                            .extracting("name", "author", "isbn")
                            .contains("자바 ORM 표준 JPA 프로그래밍", "김영한", "9788960777330"),
                    () -> assertThat(movie)
                            .extracting("name", "actor", "director")
                            .contains("범죄도시", "마동석", "강윤성")
            );
        }

        @Test
        void addStock() {
            //given
            Album album = Album.builder().stockQuantity(3).build();

            //when
            album.addStock(4);

            //then
            assertThat(album.getStockQuantity()).isEqualTo(7);
        }

        @Test
        void removeStock() {
            //given
            Album album = Album.builder().stockQuantity(3).build();

            //when
            album.removeStock(2);

            //then
            assertThat(album.getStockQuantity()).isEqualTo(1);
        }

        @Test
        void updateStock() {
            //given
            Album album = Album.builder().stockQuantity(3).build();

            //when
            album.updateStock(2, 3);

            //then
            assertThat(album.getStockQuantity()).isEqualTo(2);
        }

        @Test
        void update() {
            //given
            Album album = Album.builder().price(15000).stockQuantity(10).build();

            //when
            album.updateProduct(12000, 20);

            //then
            assertThat(album)
                    .extracting("price", "stockQuantity")
                    .contains(12000, 20);
        }

        @Test
        void update_priceNull() {
            //given
            Album album = Album.builder().price(15000).stockQuantity(10).build();

            //when
            album.updateProduct(null, 20);

            //then
            assertThat(album)
                    .extracting("price", "stockQuantity")
                    .contains(15000, 20);
        }

        @Test
        void update_stockQuantityNull() {
            //given
            Album album = Album.builder().price(15000).stockQuantity(10).build();

            //when
            album.updateProduct(12000, null);

            //then
            assertThat(album)
                    .extracting("price", "stockQuantity")
                    .contains(12000, 10);
        }

        @Test
        void connectCategory() {
            //given
            Category category = Category.createCategory(null, "가요");
            Album album = Album.builder().name("BANG BANG").build();

            ReflectionTestUtils.setField(category, "id", 1L);

            //when
            album.connectCategory(category);

            //then
            assertThat(album.getCategoryProducts())
                    .isNotEmpty()
                    .isEqualTo(category.getCategoryProducts())
                    .extracting("category.name", "product.name")
                    .containsExactly(tuple("가요", "BANG BANG"));
        }

        @Test
        void connectCategories() {
            //given
            Category category1 = Category.createCategory(null, "가요");
            Category category2 = Category.createCategory(category1, "댄스");
            Album album = Album.builder().name("BANG BANG").build();

            ReflectionTestUtils.setField(category1, "id", 1L);
            ReflectionTestUtils.setField(category2, "id", 2L);

            //when
            album.connectCategories(List.of(category2));

            //then
            assertAll(
                    () -> assertThat(album.getCategoryProducts())
                            .hasSize(2)
                            .extracting("category.name")
                            .containsExactlyInAnyOrder("가요", "댄스"),
                    () -> assertThat(category1.getCategoryProducts())
                            .isNotEmpty()
                            .isNotEqualTo(category2.getCategoryProducts())
                            .extracting("product.name")
                            .containsExactly("BANG BANG"),
                    () -> assertThat(category2.getCategoryProducts())
                            .isNotEmpty()
                            .extracting("product.name")
                            .containsExactly("BANG BANG")
            );
        }

        @Test
        void connectCategory_ignoreDuplicateParent() {
            //given
            Category category1 = Category.createCategory(null, "가요");
            Category category2 = Category.createCategory(category1, "댄스");
            Album album = Album.builder().name("BANG BANG").build();

            ReflectionTestUtils.setField(category1, "id", 1L);
            ReflectionTestUtils.setField(category2, "id", 2L);

            //when
            album.connectCategories(List.of(category1, category2));

            //then
            assertAll(
                    () -> assertThat(album.getCategoryProducts())
                            .hasSize(2)
                            .extracting("category.name")
                            .containsExactlyInAnyOrder("가요", "댄스"),
                    () -> assertThat(category1.getCategoryProducts())
                            .isNotEmpty()
                            .isNotEqualTo(category2.getCategoryProducts())
                            .extracting("product.name")
                            .containsExactly("BANG BANG"),
                    () -> assertThat(category2.getCategoryProducts())
                            .isNotEmpty()
                            .extracting("product.name")
                            .containsExactly("BANG BANG")
            );
        }

        @Test
        void removeCategoryProductsFromCategory() {
            //given
            Category category1 = Category.createCategory(null, "가요");
            Category category2 = Category.createCategory(category1, "댄스");
            Album album = new Album();

            ReflectionTestUtils.setField(category1, "id", 1L);
            ReflectionTestUtils.setField(category2, "id", 2L);

            album.connectCategories(List.of(category2));

            //when
            album.removeCategoryProductsFormCategory();

            //then
            assertAll(
                    () -> assertThat(category1.getCategoryProducts()).isEmpty(),
                    () -> assertThat(category2.getCategoryProducts()).isEmpty(),
                    () -> assertThat(album.getCategoryProducts()).hasSize(2)
            );
        }

        @Test
        void removeCategoryProductsFormCategory_idempotency() {
            //given
            Category category1 = Category.createCategory(null, "가요");
            Category category2 = Category.createCategory(category1, "댄스");
            Album album = new Album();

            ReflectionTestUtils.setField(category1, "id", 1L);
            ReflectionTestUtils.setField(category2, "id", 2L);

            album.connectCategories(List.of(category2));

            //when 첫 번째 호출
            album.removeCategoryProductsFormCategory();

            //then
            assertAll(
                    () -> assertThat(category1.getCategoryProducts()).isEmpty(),
                    () -> assertThat(category2.getCategoryProducts()).isEmpty(),
                    () -> assertThat(album.getCategoryProducts()).hasSize(2)
            );

            //when 두 번째 호출
            album.removeCategoryProductsFormCategory();

            //then
            assertAll(
                    () -> assertThat(category1.getCategoryProducts()).isEmpty(),
                    () -> assertThat(category2.getCategoryProducts()).isEmpty(),
                    () -> assertThat(album.getCategoryProducts()).hasSize(2)
            );
        }

        @Test
        void removeCategoryProduct() {
            //given
            Category category = Category.createCategory(null, "가요");
            Album album = new Album();

            ReflectionTestUtils.setField(category, "id", 1L);

            album.connectCategory(category);

            //when
            album.removeCategoryProduct(category);

            //then
            assertAll(
                    () -> assertThat(album.getCategoryProducts()).isEmpty(),
                    () -> assertThat(category.getCategoryProducts()).isEmpty()
            );
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

    @Nested
    class FailureCase {

        @Test
        void addStock_quantityNull() {
            //given
            Album album = Album.builder().stockQuantity(3).build();

            //when
            assertThatThrownBy(() -> album.addStock(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("재고가 추가될 수량이 없습니다.");

            //then
            assertThat(album.getStockQuantity()).isEqualTo(3);
        }

        @Test
        void removeStock_quantityNull() {
            //given
            Album album = Album.builder().stockQuantity(3).build();

            //when
            assertThatThrownBy(() -> album.removeStock(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("재고가 감소될 수량이 없습니다.");

            //then
            assertThat(album.getStockQuantity()).isEqualTo(3);
        }

        @Test
        void removeStock_exceed() {
            //given
            Album album = Album.builder().stockQuantity(3).build();

            //when
            assertThatThrownBy(() -> album.removeStock(4))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("재고가 부족합니다.");

            //then
            assertThat(album.getStockQuantity()).isEqualTo(3);
        }

        @Test
        void updateStock_quantityNull() {
            //given
            Album album = Album.builder().stockQuantity(3).build();

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> album.updateStock(null, 3))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("재고가 추가될 수량이 없습니다."),
                    () -> assertThatThrownBy(() -> album.updateStock(2, null))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("재고가 감소될 수량이 없습니다.")
            );

            //then
            assertThat(album.getStockQuantity()).isEqualTo(3);
        }

        @Test
        void updateStock_exceed() {
            //given
            Album album = Album.builder().stockQuantity(3).build();

            //when
            assertThatThrownBy(() -> album.updateStock(1, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("재고가 부족합니다.");

            //then
            assertThat(album.getStockQuantity()).isEqualTo(3);
        }

        @Test
        void update_priceQuantityNull() {
            //given
            Album album = Album.builder().price(15000).stockQuantity(10).build();

            //when
            assertThatThrownBy(() -> album.updateProduct(null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("수정할 가격 또는 수량이 있어야 합니다.");

            //then
            assertThat(album)
                    .extracting("price", "stockQuantity")
                    .contains(15000, 10);
        }

        @Test
        void connectCategory_categoryIdIsNull() {
            //given
            Category category = Category.createCategory(null, "가요");
            Album album = new Album();

            //when
            assertThatThrownBy(() -> album.connectCategory(category))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("식별자가 없는 잘못된 카테고리입니다.");

            //then
            assertThat(album.getCategoryProducts()).isEmpty();
        }

        @Test
        void removeCategoryProduct_categoryProductsIsEmpty() {
            //given
            Category category = Category.createCategory(null, "가요");
            Album album = new Album();

            //when
            assertThatThrownBy(() -> album.removeCategoryProduct(category))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품과 연결된 카테고리가 없습니다.");
        }

        @Test
        void removeCategoryProduct_notLinked() {
            //given
            Category category1 = Category.createCategory(null, "가요");
            Category category2 = Category.createCategory(category1, "댄스");
            Album album = new Album();

            ReflectionTestUtils.setField(category1, "id", 1L);
            ReflectionTestUtils.setField(category2, "id", 2L);

            album.connectCategory(category1);

            //when
            assertThatThrownBy(() -> album.removeCategoryProduct(category2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품이 해당 카테고리에 없습니다.");

            //then
            assertThat(album.getCategoryProducts())
                    .isNotEmpty()
                    .extracting("category.name")
                    .containsExactly("가요");
        }
    }
}