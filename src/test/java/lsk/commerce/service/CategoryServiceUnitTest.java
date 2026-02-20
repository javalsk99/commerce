package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Book;
import lsk.commerce.dto.response.CategoryDisconnectResponse;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.repository.CategoryRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anySet;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
class CategoryServiceUnitTest {

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    CategoryService categoryService;

    Category category1 = Category.createCategory(null, "컴퓨터/IT");
    Category category2 = Category.createCategory(category1, "프로그래밍 언어");
    Category category3 = Category.createCategory(category2, "Java");
    Category category4 = Category.createCategory(category1, "Python");

    List<Category> categories1 = List.of(category1);
    List<Category> categories2 = List.of(category1, category2, category3, category4);

    @Nested
    class SuccessCase {

        @Test
        void createParent() {
            //given
            given(categoryRepository.existsByCategoryName(anyString(), any())).willReturn(Collections.emptyList());

            //when
            categoryService.create("컴퓨터/IT", null);

            //then
            assertAll(
                    () -> then(categoryRepository).should().existsByCategoryName(anyString(), any()),
                    () -> then(categoryRepository).should().save(argThat(c ->
                            c.getName().equals("컴퓨터/IT") && c.getParent() == null))
            );
        }

        @Test
        void createChild() {
            //given
            given(categoryRepository.existsByCategoryName(anyString(), anyString())).willReturn(categories1);

            //when
            categoryService.create("프로그래밍 언어", "컴퓨터/IT");

            //then
            assertAll(
                    () -> then(categoryRepository).should().existsByCategoryName(anyString(), anyString()),
                    () -> then(categoryRepository).should().save(argThat(c ->
                            c.getName().equals("프로그래밍 언어") && c.getParent().getName().equals("컴퓨터/IT")))
            );
        }

        @Test
        void findByName() {
            //given
            given(categoryRepository.findAll()).willReturn(categories1);

            //when
            categoryService.findCategoryByName("컴퓨터/IT");

            //then
            then(categoryRepository).should().findAll();
        }

        @Test
        void findByNames() {
            //given
            given(categoryRepository.findAll()).willReturn(categories2);

            //when
            List<Category> categories = categoryService.findCategoryByNames("컴퓨터/IT", "프로그래밍 언어");

            //then
            assertAll(
                    () -> then(categoryRepository).should().findAll(),
                    () -> assertThat(categories)
                            .extracting("name")
                            .containsExactlyInAnyOrder("컴퓨터/IT", "프로그래밍 언어")
            );
        }

        @Test
        void findByNames_ignoreDuplicateNames() {
            //given
            given(categoryRepository.findAll()).willReturn(categories1);

            //when
            List<Category> categories = categoryService.findCategoryByNames("컴퓨터/IT", "컴퓨터/IT");

            //then
            assertAll(
                    () -> then(categoryRepository).should().findAll(),
                    () -> assertThat(categories.size()).isEqualTo(1)
            );
        }

        @Test
        void findAll() {
            //given
            given(categoryRepository.findAll()).willReturn(categories2);

            //when
            List<Category> categories = categoryService.findCategories();

            //then
            assertAll(
                    () -> then(categoryRepository).should().findAll(),
                    () -> assertThat(categories)
                            .extracting("name")
                            .containsExactlyInAnyOrder("컴퓨터/IT")
            );
        }

        @Test
        void changeParent() {
            //given
            given(categoryRepository.findAll()).willReturn(categories2);

            ReflectionTestUtils.setField(category2, "id", 2L);
            ReflectionTestUtils.setField(category4, "id", 4L);

            //when
            categoryService.changeParentCategory("Python", "프로그래밍 언어");

            //then
            assertAll(
                    () -> then(categoryRepository).should().findAll(),
                    () -> assertThat(category2.getChild().getFirst()).isEqualTo(category3),
                    () -> assertThat(category3.getParent()).isEqualTo(category2)
            );
        }

        @Test
        void changeParent_hasChild() {
            //given
            given(categoryRepository.findAll()).willReturn(categories2);

            ReflectionTestUtils.setField(category2, "id", 2L);
            ReflectionTestUtils.setField(category4, "id", 4L);

            //when
            categoryService.changeParentCategory("프로그래밍 언어", "Python");

            //then
            assertAll(
                    () -> then(categoryRepository).should().findAll(),
                    () -> assertThat(category2.getParent()).isEqualTo(category4),
                    () -> assertThat(category2.getParent().getParent()).isEqualTo(category1),
                    () -> assertThat(category2.getChild())
                            .extracting("name")
                            .containsExactlyInAnyOrder("Java")
            );
        }

        @Test
        void delete() {
            //given
            given(categoryRepository.findWithChild(anyString())).willReturn(Optional.of(category4));

            //when
            categoryService.deleteCategory("Python");

            //then
            assertAll(
                    () -> then(categoryRepository).should().findWithChild(anyString()),
                    () -> then(categoryRepository).should().delete(category4)
            );
        }

        @Test
        void changeDto() {
            //given
            Category category = connectProduct();

            //when
            CategoryResponse categoryDto = categoryService.getCategoryDto(category2);
            CategoryDisconnectResponse categoryDisconnectResponse = categoryService.getCategoryDisconnectResponse(category);

            //then
            assertAll(
                    () -> assertThat(categoryDto.getName()).isEqualTo(category2.getName()),
                    () -> assertThat(categoryDto.getChild())
                            .extracting("name")
                            .containsExactly(category3.getName())
            );
            assertAll(
                    () -> assertThat(categoryDisconnectResponse.getName()).isEqualTo(category.getName()),
                    () -> assertThat(categoryDisconnectResponse.getProducts().size()).isEqualTo(1)
            );
        }

        @Test
        void validateAndGet() {
            //given
            given(categoryRepository.findByNameSet(anySet())).willReturn(List.of(category3, category4));

            //when
            List<Category> categories = categoryService.validateAndGetCategories(List.of("Java", "Python"));

            //then
            assertAll(
                    () -> then(categoryRepository).should().findByNameSet(anySet()),
                    () -> assertThat(categories)
                            .extracting("name")
                            .containsExactlyInAnyOrder("Java", "Python")
            );
        }
        @Test
        void validateAndGet_ignoreDuplicateNames() {
            //given
            given(categoryRepository.findByNameSet(anySet())).willReturn(List.of(category3, category4));

            //when
            List<Category> categories = categoryService.validateAndGetCategories(List.of("Java", "Python", "Java"));

            //then
            assertAll(
                    () -> then(categoryRepository).should().findByNameSet(anySet()),
                    () -> assertThat(categories)
                            .extracting("name")
                            .containsExactlyInAnyOrder("Java", "Python")
            );
        }

        private Category connectProduct() {
            Product product = mock(Book.class);

            CategoryProduct categoryProduct = mock(CategoryProduct.class);
            given(categoryProduct.getProduct()).willReturn(product);

            List<CategoryProduct> categoryProducts = new ArrayList<>();
            categoryProducts.add(categoryProduct);

            Category category = mock(Category.class);
            given(category.getCategoryProducts()).willReturn(categoryProducts);

            return category;
        }
    }

    @Nested
    class FailureCase {

        @Test
        void create_existsName() {
            //given
            given(categoryRepository.existsByCategoryName(anyString(), any())).willReturn(categories1);

            //when
            assertThatThrownBy(() -> categoryService.create("컴퓨터/IT", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 존재하는 카테고리입니다. name: " + "컴퓨터/IT");

            //then
            assertAll(
                    () -> then(categoryRepository).should().existsByCategoryName(anyString(), any()),
                    () -> then(categoryRepository).should(never()).save(any())
            );
        }

        @Test
        void createChild_notExistsParent() {
            //given
            given(categoryRepository.existsByCategoryName(anyString(), anyString())).willReturn(Collections.emptyList());

            //when
            assertThatThrownBy(() -> categoryService.create("프로그래밍 언어", "컴퓨터/IT"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 카테고리입니다. name: " + "컴퓨터/IT");

            //then
            assertAll(
                    () -> then(categoryRepository).should().existsByCategoryName(anyString(), anyString()),
                    () -> then(categoryRepository).should(never()).save(any())
            );
        }

        @Test
        void findByName_notExistsCategory() {
            //given
            given(categoryRepository.findAll()).willReturn(categories1);

            //when
            assertThatThrownBy(() -> categoryService.findCategoryByName("프로그래밍 언어"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 카테고리입니다. name: " + "프로그래밍 언어");

            //then
            then(categoryRepository).should().findAll();
        }

        @Test
        void findByNames_notExistsCategory() {
            //given
            given(categoryRepository.findAll()).willReturn(categories1);

            //when
            assertThatThrownBy(() -> categoryService.findCategoryByNames("컴퓨터/IT", "프로그래밍 언어"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 카테고리입니다. name: " + "프로그래밍 언어");

            //then
            then(categoryRepository).should().findAll();
        }

        @ParameterizedTest(name = "[{index}] {1}")
        @MethodSource("nameProvider")
        void changeParent_notExistsCategory(String name, String reason) {
            //given
            given(categoryRepository.findAll()).willReturn(List.of(category2, category3));

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> categoryService.changeParentCategory(name, "프로그래밍 언어"))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("존재하지 않는 카테고리입니다. name: " + name),
                    () -> assertThatThrownBy(() -> categoryService.changeParentCategory("Java", name))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("존재하지 않는 카테고리입니다. name: " + name)
            );

            //then
            then(categoryRepository).should(times(2)).findAll();
        }

        @Test
        void delete_notExistsCategory() {
            //given
            given(categoryRepository.findWithChild(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> categoryService.deleteCategory("C++"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 카테고리입니다. name: " + "C++");

            //then
            assertAll(
                    () -> then(categoryRepository).should().findWithChild(anyString()),
                    () -> then(categoryRepository).should(never()).delete(any())
            );
        }

        @Test
        void delete_hasChild() {
            //given
            given(categoryRepository.findWithChild(anyString())).willReturn(Optional.of(category2));

            //when
            assertThatThrownBy(() -> categoryService.deleteCategory("프로그래밍 언어"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("자식 카테고리가 있어서 삭제할 수 없습니다.");

            //then
            assertAll(
                    () -> then(categoryRepository).should().findWithChild(anyString()),
                    () -> then(categoryRepository).should(never()).delete(any())
            );
        }

        @Test
        void delete_hasProduct() {
            //given
            ReflectionTestUtils.setField(category4, "categoryProducts", List.of(mock(CategoryProduct.class)));

            given(categoryRepository.findWithChild(anyString())).willReturn(Optional.of(category4));

            //when
            assertThatThrownBy(() -> categoryService.deleteCategory("Python"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("카테고리에 상품이 있어서 삭제할 수 없습니다.");

            //then
            assertAll(
                    () -> then(categoryRepository).should().findWithChild(anyString()),
                    () -> assertThat(category4.getCategoryProducts().size()).isEqualTo(1)
            );
        }

        @Test
        void delete_alreadyDeleted() {
            //given
            given(categoryRepository.findWithChild(anyString()))
                    .willReturn(Optional.of(category4))
                    .willReturn(Optional.empty());

            //when 첫 번째 호출
            categoryService.deleteCategory("Python");

            //then
            assertAll(
                    () -> then(categoryRepository).should(times(1)).findWithChild(anyString()),
                    () -> then(categoryRepository).should(times(1)).delete(category4)
            );

            //when 두 번째 호출
            assertThatThrownBy(() -> categoryService.deleteCategory("Python"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 카테고리입니다. name: " + "Python");

            //then
            assertAll(
                    () -> then(categoryRepository).should(times(2)).findWithChild(anyString()),
                    () -> then(categoryRepository).should(times(1)).delete(any())
            );
        }

        @Test
        void validateAndGet_notExistsCategory() {
            //when
            assertAll(
                    () -> assertThatThrownBy(() -> categoryService.validateAndGetCategories(null))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("카테고리가 존재하지 않습니다."),
                    () -> assertThatThrownBy(() -> categoryService.validateAndGetCategories(Collections.emptyList()))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("카테고리가 존재하지 않습니다.")
            );

            //then
            then(categoryRepository).should(never()).findByNameSet(any());
        }

        @Test
        void validateAndGet_sizeMismatch() {
            //given
            given(categoryRepository.findByNameSet(anySet())).willReturn(List.of(category2, category3, category4));

            //when
            assertThatThrownBy(() -> categoryService.validateAndGetCategories(List.of("Java", "Python")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 카테고리가 있습니다.");

            //then
            then(categoryRepository).should().findByNameSet(anySet());
        }

        static Stream<Arguments> nameProvider() {
            return Stream.of(
                    arguments(null, "카테고리 이름 null"),
                    arguments("", "카테고리 이름 빈 문자열"),
                    arguments(" ", "카테고리 이름 공백"),
                    arguments("C++", "존재하지 않는 카테고리 이름")
            );
        }
    }
}
