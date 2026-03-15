package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.product.Book;
import lsk.commerce.dto.request.CategoryChangeParentRequest;
import lsk.commerce.dto.request.CategoryRequest;
import lsk.commerce.dto.response.CategoryDisconnectResponse;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.exception.DataNotFoundException;
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
                CategoryRequest request = new CategoryRequest("컴퓨터/IT", null);

                given(categoryRepository.existsByCategoryNames(anyString(), any())).willReturn(Collections.emptyList());

                //when
                categoryService.create(request);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().existsByCategoryNames(anyString(), any()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should().save(argThat(c ->
                            c.getName().equals("컴퓨터/IT") && c.getParent() == null)));
                });
            }

            @Test
            void childCategory() {
                //given
                CategoryRequest request = new CategoryRequest("프로그래밍 언어", "컴퓨터/IT");

                given(categoryRepository.existsByCategoryNames(anyString(), anyString())).willReturn(categories1);

                //when
                categoryService.create(request);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().existsByCategoryNames(anyString(), anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should().save(argThat(c ->
                            c.getName().equals("프로그래밍 언어") && c.getParent().getName().equals("컴퓨터/IT"))));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void existsName() {
                //given
                CategoryRequest request = new CategoryRequest("컴퓨터/IT", null);

                given(categoryRepository.existsByCategoryNames(anyString(), any())).willReturn(categories1);

                //when & then
                thenThrownBy(() -> categoryService.create(request))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("이미 존재하는 카테고리입니다. name: " + "컴퓨터/IT");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().existsByCategoryNames(anyString(), any()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should(never()).save(any()));
                });
            }

            @Test
            void childCategory_ParentNotFound() {
                //given
                CategoryRequest request = new CategoryRequest("프로그래밍 언어", "컴퓨터/IT");

                given(categoryRepository.existsByCategoryNames(anyString(), anyString())).willReturn(Collections.emptyList());

                //when & then
                thenThrownBy(() -> categoryService.create(request))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 카테고리입니다. name: " + "컴퓨터/IT");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().existsByCategoryNames(anyString(), anyString()));
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
                categoryService.findCategoryByName("컴퓨터/IT");

                //then
                BDDMockito.then(categoryRepository).should().findAll();
            }

            @Test
            void byNames() {
                //given
                given(categoryRepository.findAll()).willReturn(categories2);

                //when
                List<Category> categories = categoryService.findCategoryByNames("컴퓨터/IT", "프로그래밍 언어");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findAll());
                    softly.then(categories)
                            .extracting("name")
                            .containsExactlyInAnyOrder("컴퓨터/IT", "프로그래밍 언어");
                });
            }

            @Test
            void byNames_IgnoreDuplicateNames() {
                //given
                given(categoryRepository.findAll()).willReturn(categories1);

                //when
                List<Category> categories = categoryService.findCategoryByNames("컴퓨터/IT", "컴퓨터/IT");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findAll());
                    softly.then(categories.size()).isEqualTo(1);
                });
            }

            @Test
            void all() {
                //given
                given(categoryRepository.findAll()).willReturn(categories2);

                //when
                List<Category> categories = categoryService.findCategories();

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
                thenThrownBy(() -> categoryService.findCategoryByName("프로그래밍 언어"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 카테고리입니다. name: " + "프로그래밍 언어");

                //then
                BDDMockito.then(categoryRepository).should().findAll();
            }

            @Test
            void byNames_CategoryNotFound() {
                //given
                given(categoryRepository.findAll()).willReturn(categories1);

                //when & then
                thenThrownBy(() -> categoryService.findCategoryByNames("컴퓨터/IT", "프로그래밍 언어"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 카테고리입니다. name: " + "프로그래밍 언어");

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
                CategoryChangeParentRequest request = new CategoryChangeParentRequest("프로그래밍 언어");

                given(categoryRepository.findAll()).willReturn(categories2);

                //when
                categoryService.changeParentCategory("Python", request);

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
                CategoryChangeParentRequest request = new CategoryChangeParentRequest("Python");

                given(categoryRepository.findAll()).willReturn(categories2);

                //when
                categoryService.changeParentCategory("프로그래밍 언어", request);

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
            @MethodSource("nameProvider")
            void categoryNotFound(String name) {
                //given
                CategoryChangeParentRequest request1 = new CategoryChangeParentRequest("프로그래밍 언어");
                CategoryChangeParentRequest request2 = new CategoryChangeParentRequest(name);

                given(categoryRepository.findAll()).willReturn(List.of(category2, category3));

                //when & then
                thenSoftly(softly -> {
                    softly.thenThrownBy(() -> categoryService.changeParentCategory(name, request1))
                            .isInstanceOf(DataNotFoundException.class)
                            .hasMessage("존재하지 않는 카테고리입니다. name: " + name);
                    softly.thenThrownBy(() -> categoryService.changeParentCategory("Java", request2))
                            .isInstanceOf(DataNotFoundException.class)
                            .hasMessage("존재하지 않는 카테고리입니다. name: " + name);
                });

                //then
                BDDMockito.then(categoryRepository).should(times(2)).findAll();
            }

            static Stream<Arguments> nameProvider() {
                return Stream.of(
                        argumentSet("카테고리 이름 null", (Object) null),
                        argumentSet("카테고리 이름 빈 문자열", ""),
                        argumentSet("카테고리 이름 공백", " "),
                        argumentSet("존재하지 않는 카테고리 이름", "C++")
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
        }

        @Nested
        class FailureCase {

            @Test
            void categoryNotFound() {
                //given
                given(categoryRepository.findWithChild(anyString())).willReturn(Optional.empty());

                //when & then
                thenThrownBy(() -> categoryService.deleteCategory("C++"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 카테고리입니다. name: " + "C++");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findWithChild(anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should(never()).delete(any()));
                });
            }

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

            @Test
            void alreadyDeleted() {
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
                thenThrownBy(() -> categoryService.deleteCategory("Python"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 카테고리입니다. name: " + "Python");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should(times(2)).findWithChild(anyString()));
                    softly.check(() -> BDDMockito.then(categoryRepository).should().delete(category4));
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
                    softly.then(categoryDisconnectResponse.getName()).isEqualTo(category1.getName());
                    softly.then(categoryDisconnectResponse.getProducts())
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
                given(categoryRepository.findByNameSet(anySet())).willReturn(List.of(category3, category4));

                //when
                List<Category> categories = categoryService.validateAndGetCategories(List.of("Java", "Python"));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findByNameSet(anySet()));
                    softly.then(categories)
                            .extracting("name")
                            .containsExactlyInAnyOrder("Java", "Python");
                });
            }

            @Test
            void ignoreDuplicateNames() {
                //given
                given(categoryRepository.findByNameSet(anySet())).willReturn(List.of(category3, category4));

                //when
                List<Category> categories = categoryService.validateAndGetCategories(List.of("Java", "Python", "Java"));

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(categoryRepository).should().findByNameSet(anySet()));
                    softly.then(categories)
                            .extracting("name")
                            .containsExactlyInAnyOrder("Java", "Python");
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void categoryNotFound() {
                //when & then
                thenSoftly(softly -> {
                    softly.thenThrownBy(() -> categoryService.validateAndGetCategories(null))
                            .isInstanceOf(DataNotFoundException.class)
                            .hasMessage("카테고리가 존재하지 않습니다");
                    softly.thenThrownBy(() -> categoryService.validateAndGetCategories(Collections.emptyList()))
                            .isInstanceOf(DataNotFoundException.class)
                            .hasMessage("카테고리가 존재하지 않습니다");
                });

                //then
                BDDMockito.then(categoryRepository).should(never()).findByNameSet(any());
            }

            @Test
            void sizeMismatch() {
                //given
                given(categoryRepository.findByNameSet(anySet())).willReturn(List.of(category2, category3, category4));

                //when & then
                thenThrownBy(() -> categoryService.validateAndGetCategories(List.of("Java", "Python")))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 카테고리가 있습니다");

                //then
                BDDMockito.then(categoryRepository).should().findByNameSet(anySet());
            }
        }
    }
}
