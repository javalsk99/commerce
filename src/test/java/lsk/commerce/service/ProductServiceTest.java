package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.ProductChangeRequest;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.dto.response.ProductDetailResponse;
import lsk.commerce.dto.response.ProductNameWithCategoryNameResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.exception.DuplicateResourceException;
import lsk.commerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenNoException;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyList;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    CategoryService categoryService;

    @InjectMocks
    ProductService productService;

    @Captor
    ArgumentCaptor<Product> productCaptor;

    Category category1;
    Category category2;
    Category category3;

    String productNumber = "div973bkzi95";

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
    class Request {

        @Nested
        class SuccessCase {

            @Test
            void album() {
                //given
                ProductCreateRequest request = ProductCreateRequest.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .dtype("A")
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();

                given(productRepository.existsAlbum(anyString(), anyString(), anyString())).willReturn(false);
                given(categoryService.validateAndGetCategories(anyList())).willReturn(List.of(category1));

                //when
                productService.register(request, List.of("가요"));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productRepository).should().existsAlbum(anyString(), anyString(), anyString()));
                    softly.check(() -> BDDMockito.then(categoryService).should().validateAndGetCategories(List.of("가요")));
                    softly.check(() -> BDDMockito.then(productRepository).should().save(productCaptor.capture()));
                });

                Product product = productCaptor.getValue();
                thenSoftly(softly -> {
                    softly.then(product)
                            .extracting("name", "artist", "studio")
                            .containsExactly("BANG BANG", "IVE", "STARSHIP");
                    softly.then(product.getCategoryProducts())
                            .isNotEmpty()
                            .extracting("category.name")
                            .containsExactly("가요");
                });
            }

            @Test
            void book() {
                //given
                ProductCreateRequest request = ProductCreateRequest.builder()
                        .name("자바 ORM 표준 JPA 프로그래밍")
                        .price(15000)
                        .stockQuantity(10)
                        .dtype("B")
                        .author("김영한")
                        .isbn("9788960777330")
                        .build();

                given(productRepository.existsBook(anyString(), anyString(), anyString())).willReturn(false);
                given(categoryService.validateAndGetCategories(anyList())).willReturn(List.of(category2));

                //when
                productService.register(request, List.of("컴퓨터/IT"));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productRepository).should().existsBook(anyString(), anyString(), anyString()));
                    softly.check(() -> BDDMockito.then(categoryService).should().validateAndGetCategories(List.of("컴퓨터/IT")));
                    softly.check(() -> BDDMockito.then(productRepository).should().save(productCaptor.capture()));
                });

                Product product = productCaptor.getValue();
                thenSoftly(softly -> {
                    softly.then(product)
                            .extracting("name", "author", "isbn")
                            .containsExactly("자바 ORM 표준 JPA 프로그래밍", "김영한", "9788960777330");
                    softly.then(product.getCategoryProducts())
                            .isNotEmpty()
                            .extracting("category.name")
                            .containsExactly("컴퓨터/IT");
                });
            }

            @Test
            void movie() {
                //given
                ProductCreateRequest request = ProductCreateRequest.builder()
                        .name("범죄도시")
                        .price(15000)
                        .stockQuantity(10)
                        .dtype("M")
                        .actor("마동석")
                        .director("강윤성")
                        .build();

                given(productRepository.existsMovie(anyString(), anyString(), anyString())).willReturn(false);
                given(categoryService.validateAndGetCategories(anyList())).willReturn(List.of(category3));

                //when
                productService.register(request, List.of("국내 영화"));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productRepository).should().existsMovie(anyString(), anyString(), anyString()));
                    softly.check(() -> BDDMockito.then(categoryService).should().validateAndGetCategories(List.of("국내 영화")));
                    softly.check(() -> BDDMockito.then(productRepository).should().save(productCaptor.capture()));
                });

                Product product = productCaptor.getValue();
                thenSoftly(softly -> {
                    softly.then(product)
                            .extracting("name", "actor", "director")
                            .containsExactly("범죄도시", "마동석", "강윤성");
                    softly.then(product.getCategoryProducts())
                            .isNotEmpty()
                            .extracting("category.name")
                            .containsExactly("국내 영화");
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void duplicateProduct() {
                //given
                ProductCreateRequest request = ProductCreateRequest.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .dtype("A")
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();

                given(productRepository.existsAlbum(anyString(), anyString(), anyString())).willReturn(true);

                //when & then
                thenThrownBy(() -> productService.register(request, List.of("가요")))
                        .isInstanceOf(DuplicateResourceException.class)
                        .hasMessage("이미 존재하는 상품입니다. name: " + "BANG BANG");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productRepository).should().existsAlbum(anyString(), anyString(), anyString()));
                    softly.check(() -> BDDMockito.then(categoryService).should(never()).validateAndGetCategories(any()));
                    softly.check(() -> BDDMockito.then(productRepository).should(never()).save(any()));
                });
            }

            @Test
            void failedValidateCategories() {
                //given
                ProductCreateRequest request = ProductCreateRequest.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .dtype("A")
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();

                given(productRepository.existsAlbum(anyString(), anyString(), anyString())).willReturn(false);
                given(categoryService.validateAndGetCategories(anyList())).willThrow(new IllegalArgumentException());

                //when & then
                thenThrownBy(() -> productService.register(request, List.of("록")))
                        .isInstanceOf(IllegalArgumentException.class);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productRepository).should().existsAlbum(anyString(), anyString(), anyString()));
                    softly.check(() -> BDDMockito.then(categoryService).should().validateAndGetCategories(anyList()));
                    softly.check(() -> BDDMockito.then(productRepository).should(never()).save(any()));
                });
            }
        }
    }

    @Nested
    class Find {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = Album.builder()
                        .name("BANG BANG")
                        .build();

                given(productRepository.findByNumber(anyString())).willReturn(Optional.of(album));

                //when
                Product findProduct = productService.findProduct(productNumber);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productRepository).should().findByNumber(anyString()));
                    softly.then(findProduct.getName()).isEqualTo("BANG BANG");
                });
            }

            @Test
            void all() {
                //given
                Album album = Album.builder()
                        .name("BANG BANG")
                        .build();
                Book book = Book.builder()
                        .name("자바 ORM 표준 JPA 프로그래밍")
                        .build();
                Movie movie = Movie.builder()
                        .name("범죄도시")
                        .build();

                given(productRepository.findAll()).willReturn(List.of(album, book, movie));

                //when
                List<Product> products = productService.findProducts();

                //then
                then(products)
                        .hasSize(3)
                        .extracting("name")
                        .containsExactlyInAnyOrder("BANG BANG", "자바 ORM 표준 JPA 프로그래밍", "범죄도시");
            }
        }

        @Nested
        class FailureCase {

            @Test
            void byName_ProductNotFound() {
                //given
                given(productRepository.findByNumber(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> productService.findProduct("lllIIIll00OO"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 상품입니다. productNumber: " + "lllIIIll00OO");

                //then
                BDDMockito.then(productRepository).should().findByNumber(anyString());
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
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
                ProductChangeRequest request = new ProductChangeRequest(12000, 20);

                given(productRepository.findByNumber(anyString())).willReturn(Optional.of(album));

                //when
                productService.changePriceAndStock(productNumber, request);

                //then
                then(album)
                        .extracting("price", "stockQuantity")
                        .containsExactly(12000, 20);
            }
        }
    }

    @Nested
    class Delete {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();

                given(productRepository.findWithCategoryProductCategory(anyString())).willReturn(Optional.of(album));

                //when
                productService.deleteProduct(productNumber);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productRepository).should().findWithCategoryProductCategory(anyString()));
                    softly.check(() -> BDDMockito.then(productRepository).should().delete(album));
                });
            }

            @Test
            void shouldIgnoreDelete_WhenProductNotFound() {
                //given
                given(productRepository.findWithCategoryProductCategory(anyString())).willReturn(Optional.empty());

                //when & then
                productService.deleteProduct("lllIIIll00OO");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productRepository).should().findWithCategoryProductCategory(anyString()));
                    softly.check(() -> BDDMockito.then(productRepository).should(never()).delete(any()));
                });
            }

            @Test
            void idempotency() {
                //given
                Album album = Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();

                given(productRepository.findWithCategoryProductCategory(anyString()))
                        .willReturn(Optional.of(album))
                        .willReturn(Optional.empty());

                //when 첫 번째 호출
                productService.deleteProduct(productNumber);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productRepository).should().findWithCategoryProductCategory(anyString()));
                    softly.check(() -> BDDMockito.then(productRepository).should().delete(album));
                });

                //when & then 두 번째 호출
                thenNoException().isThrownBy(() -> productService.deleteProduct(productNumber));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(productRepository).should(times(2)).findWithCategoryProductCategory(anyString()));
                    softly.check(() -> BDDMockito.then(productRepository).should().delete(any()));
                });
            }
        }
    }

    @Nested
    class ChangeDto {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Album album = Album.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();

                //when
                ProductDetailResponse productDto = productService.getProductDto(album);

                //then
                then(productDto)
                        .extracting("name", "price", "stockQuantity", "artist", "studio")
                        .containsExactly("BANG BANG", 15000, 10, "IVE", "STARSHIP");
            }

            @Test
            void withCategory() {
                //given
                Book book = Book.builder()
                        .name("자바 ORM 표준 JPA 프로그래밍")
                        .build();

                book.connectCategory(category2);

                //when
                ProductNameWithCategoryNameResponse productWithCategoryDto = productService.getProductWithCategoryDto(book);

                //then
                then(productWithCategoryDto.categoryNameResponseList())
                        .isNotEmpty()
                        .extracting("categoryName")
                        .containsExactly("컴퓨터/IT");
            }
        }
    }
}
