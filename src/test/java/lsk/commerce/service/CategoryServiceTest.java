package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.product.Book;
import lsk.commerce.dto.request.CategoryChangeParentRequest;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.response.CategoryDisconnectResponse;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.exception.DuplicateResourceException;
import lsk.commerce.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.thenNoException;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anySet;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    CategoryService categoryService;

    Category category1;
    Category category2;
    Category category3;
    Category category4;

    List<Category> categories1;
    List<Category> categories2;

    @BeforeEach
    void beforeEach() {
        category1 = Category.createCategory(null, "컴퓨터/IT");
        category2 = Category.createCategory(category1, "프로그래밍 언어");
        category3 = Category.createCategory(category2, "Java");
        category4 = Category.createCategory(category1, "Python");

        ReflectionTestUtils.setField(category1, "id", 1L);
        ReflectionTestUtils.setField(category2, "id", 2L);
        ReflectionTestUtils.setField(category3, "id", 3L);
        ReflectionTestUtils.setField(category4, "id", 4L);

        categories1 = List.of(category1);
        categories2 = List.of(category1, category2, category3, category4);
    }

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void parentCategory() {
                //given
                CategoryCreateRequest request = new CategoryCreateRequest("컴퓨터/IT", null);

                given(categoryRepository.findWithParent(anyString(), any())).willReturn(Collections.emptyList());

                //when
                categoryService.create(request);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithParent(anyString(), any()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should().save(argThat(c ->
                            c.getName().equals("컴퓨터/IT") && c.getParent() == null)));
                });
            }

            @Test
            void childCategory() {
                //given
                CategoryCreateRequest request = new CategoryCreateRequest("프로그래밍 언어", category1.getCategoryNumber());

                given(categoryRepository.findWithParent(anyString(), anyString())).willReturn(categories1);

                //when
                categoryService.create(request);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithParent(anyString(), anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should().save(argThat(c ->
                            c.getName().equals("프로그래밍 언어") && c.getParent().getName().equals("컴퓨터/IT"))));
                });
            }

            @Test
            void existsName_WhenParentNumberIsDifferent() {
                //given
                CategoryCreateRequest request = new CategoryCreateRequest("Java", category1.getCategoryNumber());

                given(categoryRepository.findWithParent(anyString(), any())).willReturn(categories1);

                //when & then
                categoryService.create(request);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithParent(anyString(), anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should().save(argThat(c ->
                            c.getName().equals("Java") && c.getParent().getName().equals("컴퓨터/IT"))));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void childCategory_ParentNotFound() {
                //given
                CategoryCreateRequest request = new CategoryCreateRequest("프로그래밍 언어", "llII11OO00OO");

                given(categoryRepository.findWithParent(anyString(), anyString())).willReturn(Collections.emptyList());

                //when & then
                thenThrownBy(() -> categoryService.create(request))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("부모 카테고리가 존재하지 않습니다. parentNumber: " + "llII11OO00OO");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithParent(anyString(), anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should(never()).save(any()));
                });
            }

            @Test
            void existsName_WhenParentNumberIsSameNull() {
                //given
                CategoryCreateRequest request = new CategoryCreateRequest("컴퓨터/IT", null);

                given(categoryRepository.findWithParent(anyString(), any())).willReturn(categories1);

                //when & then
                thenThrownBy(() -> categoryService.create(request))
                        .isInstanceOf(DuplicateResourceException.class)
                        .hasMessage("이미 존재하는 카테고리입니다. name: " + "컴퓨터/IT");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithParent(anyString(), any()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should(never()).save(any()));
                });
            }

            @Test
            void childCategory_WithSameNameAsParent() {
                //given
                CategoryCreateRequest request = new CategoryCreateRequest("컴퓨터/IT", category1.getCategoryNumber());

                given(categoryRepository.findWithParent(anyString(), anyString())).willReturn(categories1);

                //when & then
                thenThrownBy(() -> categoryService.create(request))
                        .isInstanceOf(DuplicateResourceException.class)
                        .hasMessage("부모 카테고리와 같은 이름입니다. name: " + "컴퓨터/IT");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithParent(anyString(), anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should(never()).save(any()));
                });
            }

            @Test
            void existsName_WhenParentNumberIsSame() {
                //given
                CategoryCreateRequest request = new CategoryCreateRequest("프로그래밍 언어", category1.getCategoryNumber());

                given(categoryRepository.findWithParent(anyString(), any())).willReturn(List.of(category1, category2));

                //when & then
                thenThrownBy(() -> categoryService.create(request))
                        .isInstanceOf(DuplicateResourceException.class)
                        .hasMessage("이미 존재하는 카테고리입니다. name: " + "프로그래밍 언어");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithParent(anyString(), any()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should(never()).save(any()));
                });
            }
        }
    }

    @Nested
    class Find {

        @Nested
        class SuccessCase {

            @Test
            void byName() {
                //given
                given(categoryRepository.findAll()).willReturn(categories1);

                //when
                categoryService.findCategoryByCategoryNumber(category1.getCategoryNumber());

                //then
                BDDMockito.then(categoryRepository).should().findAll();
            }

            @Test
            void all() {
                //given
                given(categoryRepository.findAll()).willReturn(categories2);

                //when
                List<Category> categories = categoryService.findRootCategories();

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findAll());
                    softly.then(categories)
                            .extracting("name")
                            .containsExactlyInAnyOrder("컴퓨터/IT");
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void byName_CategoryNotFound() {
                //given
                given(categoryRepository.findAll()).willReturn(categories1);

                //when & then
                thenThrownBy(() -> categoryService.findCategoryByCategoryNumber(category2.getCategoryNumber()))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 카테고리입니다. categoryNumber: " + category2.getCategoryNumber());

                //then
                BDDMockito.then(categoryRepository).should().findAll();
            }
        }
    }

    @Nested
    class ChangeParent {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                CategoryChangeParentRequest request = new CategoryChangeParentRequest(category2.getCategoryNumber());

                given(categoryRepository.findAll()).willReturn(categories2);

                //when
                categoryService.changeParentCategory(category3.getCategoryNumber(), request);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findAll());
                    softly.then(category2.getChildren().getFirst()).isEqualTo(category3);
                    softly.then(category3.getParent()).isEqualTo(category2);
                });
            }

            @Test
            void hasChild() {
                //given
                CategoryChangeParentRequest request = new CategoryChangeParentRequest(category4.getCategoryNumber());

                given(categoryRepository.findAll()).willReturn(categories2);

                //when
                categoryService.changeParentCategory(category2.getCategoryNumber(), request);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findAll());
                    softly.then(category2.getParent()).isEqualTo(category4);
                    softly.then(category2.getParent().getParent()).isEqualTo(category1);
                    softly.then(category2.getChildren())
                            .extracting("name")
                            .containsExactlyInAnyOrder("Java");
                });
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("numberProvider")
            void categoryNotFound(String categoryNumber) {
                //given
                CategoryChangeParentRequest request1 = new CategoryChangeParentRequest(category2.getCategoryNumber());
                CategoryChangeParentRequest request2 = new CategoryChangeParentRequest(categoryNumber);

                given(categoryRepository.findAll()).willReturn(List.of(category2, category3));

                //when & then
                thenSoftly(softly -> {
                    softly.thenThrownBy(() -> categoryService.changeParentCategory(categoryNumber, request1))
                            .isInstanceOf(DataNotFoundException.class)
                            .hasMessage("존재하지 않는 카테고리입니다. categoryNumber: " + categoryNumber);
                    softly.thenThrownBy(() -> categoryService.changeParentCategory(category3.getCategoryNumber(), request2))
                            .isInstanceOf(DataNotFoundException.class)
                            .hasMessage("부모 카테고리가 존재하지 않습니다. parentNumber: " + categoryNumber);
                });

                //then
                BDDMockito.then(categoryRepository).should(times(2)).findAll();
            }

            static Stream<Arguments> numberProvider() {
                return Stream.of(
                        argumentSet("카테고리 번호 null", (Object) null),
                        argumentSet("카테고리 번호 빈 문자열", ""),
                        argumentSet("카테고리 번호 공백", " "),
                        argumentSet("존재하지 않는 카테고리 번호", "llII11OO00OO")
                );
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
                given(categoryRepository.findWithChild(anyString())).willReturn(Optional.of(category4));

                //when
                categoryService.deleteCategory("Python");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithChild(anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should().delete(category4));
                });
            }

            @Test
            void shouldIgnoreDelete_WhenCategoryNotFound() {
                //given
                given(categoryRepository.findWithChild(anyString())).willReturn(Optional.empty());

                //when & then
                categoryService.deleteCategory("C++");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithChild(anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should(never()).delete(any()));
                });
            }

            @Test
            void idempotency() {
                //given
                given(categoryRepository.findWithChild(anyString()))
                        .willReturn(Optional.of(category4))
                        .willReturn(Optional.empty());

                //when 첫 번째 호출
                categoryService.deleteCategory("Python");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithChild(anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should().delete(category4));
                });

                //when & then 두 번째 호출
                thenNoException().isThrownBy(() -> categoryService.deleteCategory("Python"));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should(times(2)).findWithChild(anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should().delete(any()));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void hasChild() {
                //given
                given(categoryRepository.findWithChild(anyString())).willReturn(Optional.of(category2));

                //when & then
                thenThrownBy(() -> categoryService.deleteCategory("프로그래밍 언어"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("자식 카테고리가 있어서 삭제할 수 없습니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithChild(anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should(never()).delete(any()));
                });
            }

            @Test
            void hasProduct() {
                //given
                Book book = Book.builder()
                        .name("자바 ORM 표준 JPA 프로그래밍")
                        .price(15000)
                        .stockQuantity(7)
                        .author("김영한")
                        .isbn("9788960777330")
                        .build();
                book.connectCategory(category4);

                given(categoryRepository.findWithChild(anyString())).willReturn(Optional.of(category4));

                //when & then
                thenThrownBy(() -> categoryService.deleteCategory("Python"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("카테고리에 상품이 있어서 삭제할 수 없습니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithChild(anyString()));
                    softly.then(category4.getCategoryProducts())
                            .isNotEmpty()
                            .extracting("category.name", "product.name", "product.author")
                            .containsExactly(tuple("Python", "자바 ORM 표준 JPA 프로그래밍", "김영한"));
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
                Book book = Book.builder()
                        .name("자바 ORM 표준 JPA 프로그래밍")
                        .price(15000)
                        .stockQuantity(7)
                        .author("김영한")
                        .isbn("9788960777330")
                        .build();
                book.connectCategory(category1);

                //when
                CategoryResponse categoryDto = categoryService.getCategoryDto(category2);
                CategoryDisconnectResponse categoryDisconnectResponse = categoryService.getCategoryDisconnectResponse(category1);

                //then
                thenSoftly(softly -> {
                    softly.then(categoryDto.name()).isEqualTo(category2.getName());
                    softly.then(categoryDto.children())
                            .extracting("name")
                            .containsExactly(category3.getName());
                });
                thenSoftly(softly -> {
                    softly.then(categoryDisconnectResponse.name()).isEqualTo(category1.getName());
                    softly.then(categoryDisconnectResponse.productResponseList())
                            .isNotEmpty()
                            .extracting("name", "author")
                            .containsExactly(tuple("자바 ORM 표준 JPA 프로그래밍", "김영한"));
                });
            }
        }
    }

    @Nested
    class ValidateAndGet {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                given(categoryRepository.findByNumberSet(anySet())).willReturn(List.of(category3, category4));

                //when
                List<Category> categories = categoryService.validateAndGetCategories(List.of(category3.getCategoryNumber(), category4.getCategoryNumber()));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findByNumberSet(anySet()));
                    softly.then(categories)
                            .extracting("name")
                            .containsExactlyInAnyOrder("Java", "Python");
                });
            }

            @Test
            void ignoreDuplicateNumbers() {
                //given
                given(categoryRepository.findByNumberSet(anySet())).willReturn(List.of(category3, category4));

                //when
                List<Category> categories = categoryService.validateAndGetCategories(List.of(category3.getCategoryNumber(), category4.getCategoryNumber(), category3.getCategoryNumber()));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findByNumberSet(anySet()));
                    softly.then(categories)
                            .extracting("name")
                            .containsExactlyInAnyOrder("Java", "Python");
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void sizeMismatch() {
                //given
                given(categoryRepository.findByNumberSet(anySet())).willReturn(List.of(category2, category3, category4));

                //when & then
                thenThrownBy(() -> categoryService.validateAndGetCategories(List.of(category3.getCategoryNumber(), category4.getCategoryNumber())))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 카테고리가 있습니다");

                //then
                BDDMockito.then(categoryRepository).should().findByNumberSet(anySet());
            }
        }
    }
}
