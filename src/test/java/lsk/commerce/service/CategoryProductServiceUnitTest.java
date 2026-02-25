package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.domain.product.Album;
import lsk.commerce.repository.CategoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
class CategoryProductServiceUnitTest {

    @Mock
    CategoryProductRepository categoryProductRepository;

    @Mock
    CategoryService categoryService;

    @Mock
    ProductService productService;

    @InjectMocks
    CategoryProductService categoryProductService;

    @Captor
    ArgumentCaptor<CategoryProduct> captor;

    Category category1;
    Category category2;
    Category category3;
    Category category4;
    Album album1;
    Album album2;

    @BeforeEach
    void beforeEach() {
        category1 = Category.createCategory(null, "가요");
        category2 = Category.createCategory(category1, "댄스");
        category3 = Category.createCategory(category1, "발라드");
        category4 = Category.createCategory(category1, "록");

        ReflectionTestUtils.setField(category1, "id", 1L);
        ReflectionTestUtils.setField(category2, "id", 2L);
        ReflectionTestUtils.setField(category3, "id", 3L);
        ReflectionTestUtils.setField(category4, "id", 4L);

        album1 = Album.builder().name("BANG BANG").build();
        album2 = Album.builder().name("타임 캡슐").build();

        album1.connectCategories(List.of(category1, category2));
        album2.connectCategories(List.of(category1, category3));
    }

    @Nested
    class SuccessCase {

        @Test
        void findCategoryProducts() {
            //given
            given(categoryProductRepository.findAllWithProductByCategory(any())).willReturn(category1.getCategoryProducts());

            //when
            List<CategoryProduct> categoryProducts = categoryProductService.findCategoryProductsWithProductByCategory(category1);

            //then
            assertAll(
                    () -> then(categoryProductRepository).should().findAllWithProductByCategory(any()),
                    () -> assertThat(categoryProducts)
                            .extracting("category.name", "product.name")
                            .containsExactlyInAnyOrder(tuple("가요", "BANG BANG"), tuple("가요", "타임 캡슐"))
            );
        }

        @Test
        void disconnect() {
            //given
            given(categoryService.findCategoryByName(anyString())).willReturn(category1);
            given(productService.findProductWithCategoryProduct(anyString())).willReturn(album1);

            //when
            categoryProductService.disconnect("가요", "BANG BANG");

            //then
            assertAll(
                    () -> then(categoryService).should().findCategoryByName(anyString()),
                    () -> then(productService).should().findProductWithCategoryProduct(anyString()),
                    () -> then(categoryProductRepository).should().delete(captor.capture())
            );

            CategoryProduct deletedCategoryProduct = captor.getValue();
            assertThat(deletedCategoryProduct)
                    .extracting("category.name", "product.name")
                    .containsExactly("가요", "BANG BANG");
        }

        @Test
        void disconnectAll() {
            //given
            given(categoryService.findCategoryByName(anyString())).willReturn(category1);
            given(categoryProductRepository.findAllWithProductByCategory(any())).willReturn(category1.getCategoryProducts());

            //when
            categoryProductService.disconnectAll("가요");

            //then
            assertAll(
                    () -> then(categoryService).should().findCategoryByName(anyString()),
                    () -> then(categoryProductRepository).should().findAllWithProductByCategory(any()),
                    () -> then(categoryProductRepository).should(times(2)).delete(captor.capture())
            );

            List<CategoryProduct> deletedCategoryProducts = captor.getAllValues();
            assertThat(deletedCategoryProducts)
                    .hasSize(2)
                    .extracting("category.name", "product.name")
                    .containsExactlyInAnyOrder(tuple("가요", "BANG BANG"), tuple("가요", "타임 캡슐"));
        }

        @Test
        void connect() {
            //given
            given(productService.findProductWithCategoryProduct(anyString())).willReturn(album1);
            given(categoryService.findCategoryByName(anyString())).willReturn(category3);

            //when
            categoryProductService.connect("BANG BANG", "발라드");

            //then
            assertAll(
                    () -> then(productService).should().findProductWithCategoryProduct(anyString()),
                    () -> then(categoryService).should().findCategoryByName(anyString()),
                    () -> assertThat(album1.getCategoryProducts())
                            .hasSize(3)
                            .extracting("category.name")
                            .containsExactlyInAnyOrder("가요", "댄스", "발라드")
            );
        }
    }

    @Nested
    class FailureCase {

        @Test
        void disconnect_categoryNotFound() {
            //given
            given(categoryService.findCategoryByName(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + "OST"));

            //when
            assertThatThrownBy(() -> categoryProductService.disconnect("OST", "BANG BANG"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 카테고리입니다. name: " + "OST");

            //then
            assertAll(
                    () -> then(categoryService).should().findCategoryByName(anyString()),
                    () -> then(productService).should(never()).findProductWithCategoryProduct(any()),
                    () -> then(categoryProductRepository).should(never()).delete(any())
            );
        }

        @Test
        void disconnect_productNotFound() {
            //given
            given(categoryService.findCategoryByName(anyString())).willReturn(category2);
            given(productService.findProductWithCategoryProduct(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 상품입니다. name: " + "천상연"));

            //when
            assertThatThrownBy(() -> categoryProductService.disconnect("댄스", "천상연"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 상품입니다. name: " + "천상연");

            //then
            assertAll(
                    () -> then(categoryService).should().findCategoryByName(anyString()),
                    () -> then(productService).should().findProductWithCategoryProduct(anyString()),
                    () -> then(categoryProductRepository).should(never()).delete(any())
            );
        }

        @Test
        void disconnect_alreadyDisconnect() {
            //given
            CategoryProduct categoryProduct = album1.getCategoryProducts().getFirst();

            given(categoryService.findCategoryByName(anyString())).willReturn(category1);
            given(productService.findProductWithCategoryProduct(anyString())).willReturn(album1);

            //when 첫 번째 호출
            categoryProductService.disconnect("가요", "BANG BANG");

            //then
            assertAll(
                    () -> then(categoryService).should().findCategoryByName(anyString()),
                    () -> then(productService).should().findProductWithCategoryProduct(anyString()),
                    () -> then(categoryProductRepository).should().delete(categoryProduct)
            );

            //when 두 번째 호출
            assertThatThrownBy(() -> categoryProductService.disconnect("가요", "BANG BANG"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품이 해당 카테고리에 없습니다.");

            //then
            assertAll(
                    () -> then(categoryService).should(times(2)).findCategoryByName(anyString()),
                    () -> then(productService).should(times(2)).findProductWithCategoryProduct(anyString()),
                    () -> then(categoryProductRepository).should().delete(any())
            );
        }

        @Test
        void disconnectAll_categoryNotFound() {
            //given
            given(categoryService.findCategoryByName(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + "OST"));

            //when
            assertThatThrownBy(() -> categoryProductService.disconnectAll("OST"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 카테고리입니다. name: " + "OST");

            //then
            assertAll(
                    () -> then(categoryService).should().findCategoryByName(anyString()),
                    () -> then(categoryProductRepository).should(never()).findAllWithProductByCategory(any()),
                    () -> then(categoryProductRepository).should(never()).delete(any())
            );
        }

        @Test
        void disconnectAll_categoryProductsIsEmpty_byProduct() {
            //given
            given(categoryService.findCategoryByName(anyString())).willReturn(category4);
            given(categoryProductRepository.findAllWithProductByCategory(any())).willReturn(Collections.emptyList());

            //when
            assertThatThrownBy(() -> categoryProductService.disconnectAll("록"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("카테고리에 상품이 없습니다.");

            //then
            assertAll(
                    () -> then(categoryService).should().findCategoryByName(anyString()),
                    () -> then(categoryProductRepository).should().findAllWithProductByCategory(any()),
                    () -> then(categoryProductRepository).should(never()).delete(any())
            );
        }

        @Test
        void disconnectAll_alreadyDisconnectAll() {
            //given
            given(categoryService.findCategoryByName(anyString())).willReturn(category1);
            given(categoryProductRepository.findAllWithProductByCategory(any()))
                    .willReturn(category1.getCategoryProducts())
                    .willReturn(Collections.emptyList());

            //when 첫 번째 호출
            categoryProductService.disconnectAll("가요");

            //then
            assertAll(
                    () -> then(categoryService).should().findCategoryByName(anyString()),
                    () -> then(categoryProductRepository).should().findAllWithProductByCategory(any()),
                    () -> then(categoryProductRepository).should(times(2)).delete(captor.capture())
            );

            List<CategoryProduct> deletedCategoryProducts = captor.getAllValues();
            assertThat(deletedCategoryProducts)
                    .hasSize(2)
                    .extracting("category.name", "product.name")
                    .containsExactlyInAnyOrder(tuple("가요", "BANG BANG"), tuple("가요", "타임 캡슐"));

            //when 두 번째 호출
            assertThatThrownBy(() -> categoryProductService.disconnectAll("가요"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("카테고리에 상품이 없습니다.");

            //then
            assertAll(
                    () -> then(categoryService).should(times(2)).findCategoryByName(anyString()),
                    () -> then(categoryProductRepository).should(times(2)).findAllWithProductByCategory(any()),
                    () -> then(categoryProductRepository).should(times(2)).delete(any())
            );
        }

        @Test
        void connect_productNotFound() {
            //given
            given(productService.findProductWithCategoryProduct(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 상품입니다. name: " + "천상연"));

            //when
            assertThatThrownBy(() -> categoryProductService.connect("천상연", "발라드"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 상품입니다. name: " + "천상연");

            //then
            assertAll(
                    () -> then(productService).should().findProductWithCategoryProduct(anyString()),
                    () -> then(categoryService).should(never()).findCategoryByName(any())
            );
        }

        @Test
        void connect_categoryNotFound() {
            //given
            given(productService.findProductWithCategoryProduct(anyString())).willReturn(album1);
            given(categoryService.findCategoryByName(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + "OST"));

            //when
            assertThatThrownBy(() -> categoryProductService.connect("BANG BANG", "OST"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 카테고리입니다. name: " + "OST");

            //then
            assertAll(
                    () -> then(productService).should().findProductWithCategoryProduct(anyString()),
                    () -> then(categoryService).should().findCategoryByName(anyString()),
                    () -> assertThat(album1.getCategoryProducts())
                            .hasSize(2)
                            .extracting("category.name")
                            .containsExactlyInAnyOrder("가요", "댄스")
            );
        }

        @Test
        void connect_alreadyConnected() {
            //given
            given(productService.findProductWithCategoryProduct(anyString())).willReturn(album1);
            given(categoryService.findCategoryByName(anyString())).willReturn(category3);

            //when 첫 번째 호출
            categoryProductService.connect("BANG BANG", "발라드");

            //then
            assertAll(
                    () -> then(productService).should().findProductWithCategoryProduct(anyString()),
                    () -> then(categoryService).should().findCategoryByName(anyString()),
                    () -> assertThat(album1.getCategoryProducts())
                            .hasSize(3)
                            .extracting("category.name")
                            .containsExactlyInAnyOrder("가요", "댄스", "발라드")
            );

            //when 두 번째 호출
            assertThatThrownBy(() -> categoryProductService.connect("BANG BANG", "발라드"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 상품이 해당 카테고리에 연결되어 있습니다.");

            //then
            assertAll(
                    () -> then(productService).should(times(2)).findProductWithCategoryProduct(anyString()),
                    () -> then(categoryService).should(times(2)).findCategoryByName(anyString()),
                    () -> assertThat(album1.getCategoryProducts())
                            .hasSize(3)
                            .extracting("category.name")
                            .containsExactlyInAnyOrder("가요", "댄스", "발라드")
            );
        }
    }
}
