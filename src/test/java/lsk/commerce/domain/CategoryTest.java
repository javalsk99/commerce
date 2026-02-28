package lsk.commerce.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CategoryTest {

    Category parentCategory;
    Category childCategory;

    @BeforeEach
    void beforeEach() {
        parentCategory = Category.createCategory(null, "가요");
        childCategory = Category.createCategory(parentCategory, "댄스");

        ReflectionTestUtils.setField(parentCategory, "id", 1L);
        ReflectionTestUtils.setField(childCategory, "id", 2L);
    }

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                Category parentCategory = Category.createCategory(null, "가요");
                Category childCategory = Category.createCategory(parentCategory, "댄스");

                //then
                assertAll(
                        () -> assertThat(parentCategory.getParent()).isNull(),
                        () -> assertThat(parentCategory.getChild().getFirst()).isEqualTo(childCategory),
                        () -> assertThat(childCategory.getParent()).isEqualTo(parentCategory)
                );
            }
        }
    }

    @Nested
    class UnConnect {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                childCategory.unConnectParent();

                //then
                assertAll(
                        () -> assertThat(childCategory.getParent()).isNull(),
                        () -> assertThat(parentCategory.getChild()).isEmpty()
                );
            }

            @Test
            void idempotency() {
                //when 첫 번째 호출
                childCategory.unConnectParent();

                //then
                assertAll(
                        () -> assertThat(childCategory.getParent()).isNull(),
                        () -> assertThat(parentCategory.getChild()).isEmpty()
                );

                //when 두 번째 호출
                assertDoesNotThrow(() -> childCategory.unConnectParent());

                //then
                assertThat(childCategory.getParent()).isNull();
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
                Category category = createCategory();

                ReflectionTestUtils.setField(category, "id", 3L);

                //when
                category.changeParentCategory(childCategory);

                //then
                assertAll(
                        () -> assertThat(childCategory.getChild().getFirst()).isEqualTo(category),
                        () -> assertThat(category.getParent()).isEqualTo(childCategory)
                );
            }
        }

        @Nested
        class FailureCase {

            @Test
            void selfOrChild() {
                //when
                assertAll(
                        () -> assertThatThrownBy(() -> parentCategory.changeParentCategory(parentCategory))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("자신 또는 자식을 부모로 설정할 수 없습니다."),
                        () -> assertThatThrownBy(() -> parentCategory.changeParentCategory(childCategory))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("자신 또는 자식을 부모로 설정할 수 없습니다.")
                );
            }
        }
    }

    private Category createCategory() {
        return Category.createCategory(null, "발라드");
    }
}