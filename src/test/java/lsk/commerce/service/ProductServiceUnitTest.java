package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.dto.response.ProductWithCategoryResponse;
import lsk.commerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyList;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
class ProductServiceUnitTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    CategoryService categoryService;

    @InjectMocks
    ProductService productService;

    Category category1;
    Category category2;
    Category category3;

    @BeforeEach
    void beforeEach() {
        category1 = Category.createCategory(null, "가요");
        category2 = Category.createCategory(null, "컴퓨터/IT");
        category3 = Category.createCategory(null, "국내 영화");

        ReflectionTestUtils.setField(category1, "id", 1L);
        ReflectionTestUtils.setField(category2, "id", 2L);
        ReflectionTestUtils.setField(category3, "id", 3L);
    }

    @Nested
    class SuccessCase {

        @Test
        void register_album() {
            //given
            Album album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build();

            given(productRepository.existsAlbum(anyString(), anyString(), anyString())).willReturn(false);
            given(categoryService.validateAndGetCategories(anyList())).willReturn(List.of(category1));

            //when
            productService.register(album, List.of("가요"));

            //then
            assertAll(
                    () -> then(productRepository).should().existsAlbum(anyString(), anyString(), anyString()),
                    () -> then(categoryService).should().validateAndGetCategories(List.of("가요")),
                    () -> then(productRepository).should().save(argThat(p -> p.getName().equals("BANG BANG"))),
                    () -> assertThat(album.getCategoryProducts())
                            .isNotEmpty()
                            .extracting("category.name")
                            .containsExactly("가요")
            );
        }

        @Test
        void register_book() {
            //given
            Book book = Book.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(10).author("김영한").isbn("9788960777330").build();

            given(productRepository.existsBook(anyString(), anyString(), anyString())).willReturn(false);
            given(categoryService.validateAndGetCategories(anyList())).willReturn(List.of(category2));

            //when
            productService.register(book, List.of("컴퓨터/IT"));

            //then
            assertAll(
                    () -> then(productRepository).should().existsBook(anyString(), anyString(), anyString()),
                    () -> then(categoryService).should().validateAndGetCategories(List.of("컴퓨터/IT")),
                    () -> then(productRepository).should().save(argThat(p -> p.getName().equals("자바 ORM 표준 JPA 프로그래밍"))),
                    () -> assertThat(book.getCategoryProducts())
                            .isNotEmpty()
                            .extracting("category.name")
                            .containsExactly("컴퓨터/IT")
            );
        }

        @Test
        void register_movie() {
            //given
            Movie movie = Movie.builder().name("범죄도시").price(15000).stockQuantity(10).actor("마동석").director("강윤성").build();

            given(productRepository.existsMovie(anyString(), anyString(), anyString())).willReturn(false);
            given(categoryService.validateAndGetCategories(anyList())).willReturn(List.of(category3));

            //when
            productService.register(movie, List.of("국내 영화"));

            //then
            assertAll(
                    () -> then(productRepository).should().existsMovie(anyString(), anyString(), anyString()),
                    () -> then(categoryService).should().validateAndGetCategories(List.of("국내 영화")),
                    () -> then(productRepository).should().save(argThat(p -> p.getName().equals("범죄도시"))),
                    () -> assertThat(movie.getCategoryProducts())
                            .isNotEmpty()
                            .extracting("category.name")
                            .containsExactly("국내 영화")
            );
        }

        @Test
        void find() {
            //given
            Album album = Album.builder().name("BANG BANG").build();

            given(productRepository.findByName(anyString())).willReturn(Optional.of(album));

            //when
            Product findProduct = productService.findProductByName("BANG BANG");

            //then
            assertAll(
                    () -> then(productRepository).should().findByName(anyString()),
                    () -> assertThat(findProduct.getName()).isEqualTo("BANG BANG")
            );
        }

        @Test
        void findAll() {
            //given
            Album album = Album.builder().name("BANG BANG").build();
            Book book = Book.builder().name("자바 ORM 표준 JPA 프로그래밍").build();
            Movie movie = Movie.builder().name("범죄도시").build();

            given(productRepository.findAll()).willReturn(List.of(album, book, movie));

            //when
            List<Product> products = productService.findProducts();

            //then
            assertThat(products)
                    .hasSize(3)
                    .extracting("name")
                    .containsExactlyInAnyOrder("BANG BANG", "자바 ORM 표준 JPA 프로그래밍", "범죄도시");
        }

        @Test
        void update() {
            //given
            Album album = Album.builder().name("BANG BANG").build();

            given(productRepository.findByName(anyString())).willReturn(Optional.of(album));

            //when
            productService.updateProduct("BANG BANG", 12000, 20);

            //then
            assertThat(album)
                    .extracting("price", "stockQuantity")
                    .containsExactly(12000, 20);
        }

        @Test
        void delete() {
            //given
            Album album = Album.builder().name("BANG BANG").build();

            given(productRepository.findWithCategoryProductCategory(anyString())).willReturn(Optional.of(album));

            //when
            productService.deleteProduct("BANG BANG");

            //then
            assertAll(
                    () -> then(productRepository).should().findWithCategoryProductCategory(anyString()),
                    () -> then(productRepository).should().delete(album)
            );
        }

        @Test
        void changeDto() {
            //given
            Album album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build();
            Book book = Book.builder().name("자바 ORM 표준 JPA 프로그래밍").build();

            book.connectCategory(category2);

            //when
            ProductResponse productDto = productService.getProductDto(album);
            ProductWithCategoryResponse productWithCategoryDto = productService.getProductWithCategoryDto(book);

            //then
            assertAll(
                    () -> assertThat(productDto)
                            .extracting("name", "price", "stockQuantity", "artist", "studio")
                            .containsExactly("BANG BANG", 15000, 10, "IVE", "STARSHIP"),
                    () -> assertThat(productWithCategoryDto.getCategoryNames())
                            .isNotEmpty()
                            .extracting("categoryName")
                            .containsExactly("컴퓨터/IT")
            );
        }
    }

    @Nested
    class FailureCase {

        @Test
        void register_duplicateProduct() {
            //given
            Album album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build();

            given(productRepository.existsAlbum(anyString(), anyString(), anyString())).willReturn(true);

            //when
            assertThatThrownBy(() -> productService.register(album, List.of("가요")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 존재하는 상품입니다.");

            //then
            assertAll(
                    () -> then(productRepository).should().existsAlbum(anyString(), anyString(), anyString()),
                    () -> then(categoryService).should(never()).validateAndGetCategories(any()),
                    () -> then(productRepository).should(never()).save(any())
            );
        }

        @Test
        void register_failedValidateCategories() {
            //given
            Album album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build();

            given(productRepository.existsAlbum(anyString(), anyString(), anyString())).willReturn(false);
            given(categoryService.validateAndGetCategories(anyList())).willThrow(new IllegalArgumentException());

            //when
            assertThatThrownBy(() -> productService.register(album, List.of("가요")))
                    .isInstanceOf(IllegalArgumentException.class);

            //then
            assertAll(
                    () -> then(productRepository).should().existsAlbum(anyString(), anyString(), anyString()),
                    () -> then(categoryService).should().validateAndGetCategories(anyList()),
                    () -> then(productRepository).should(never()).save(any())
            );
        }

        @Test
        void find_productNotFound() {
            //given
            given(productRepository.findByName(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> productService.findProductByName("하얀 그리움"))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("존재하지 않는 상품입니다. name: " + "하얀 그리움");

            //then
            then(productRepository).should().findByName(anyString());
        }

        @Test
        void delete_productNotFound() {
            //given
            given(productRepository.findWithCategoryProductCategory(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> productService.deleteProduct("하얀 그리움"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 상품입니다. name: " + "하얀 그리움");

            //then
            assertAll(
                    () -> then(productRepository).should().findWithCategoryProductCategory(anyString()),
                    () -> then(productRepository).should(never()).delete(any())
            );
        }

        @Test
        void delete_alreadyDeleted() {
            //given
            Album album = Album.builder().name("BANG BANG").build();

            given(productRepository.findWithCategoryProductCategory(anyString()))
                    .willReturn(Optional.of(album))
                    .willReturn(Optional.empty());

            //when 첫 번째 호출
            productService.deleteProduct("BANG BANG");

            //then
            assertAll(
                    () -> then(productRepository).should().findWithCategoryProductCategory(anyString()),
                    () -> then(productRepository).should().delete(album)
            );

            //when 두 번째 호출
            assertThatThrownBy(() -> productService.deleteProduct("BANG BANG"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 상품입니다. name: " + "BANG BANG");

            //then
            assertAll(
                    () -> then(productRepository).should(times(2)).findWithCategoryProductCategory(anyString()),
                    () -> then(productRepository).should().delete(any())
            );
        }
    }
}
