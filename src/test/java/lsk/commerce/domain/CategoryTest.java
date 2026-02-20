package lsk.commerce.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CategoryTest {

    @Nested
    class SuccessCase {

        @Test
        void create() {
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

        @Test
        void unConnect() {
            //given
            Category parentCategory = createCategory1();
            Category childCategory = createCategory2(parentCategory);

            //when
            childCategory.unConnectParent();

            //then
            assertAll(
                    () -> assertThat(childCategory.getParent()).isNull(),
                    () -> assertThat(parentCategory.getChild()).isEmpty()
            );
        }

        @Test
        void unConnect_idempotency() {
            //given
            Category parentCategory = createCategory1();
            Category childCategory = createCategory2(parentCategory);

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

        @Test
        void changeParent() {
            //given
            Category category1 = createCategory1();
            Category category2 = createCategory3();

            ReflectionTestUtils.setField(category1, "id", 1L);
            ReflectionTestUtils.setField(category2, "id", 2L);

            //when
            category2.changeParentCategory(category1);

            //then
            assertAll(
                    () -> assertThat(category1.getChild().getFirst()).isEqualTo(category2),
                    () -> assertThat(category2.getParent()).isEqualTo(category1)
            );
        }
    }

    @Nested
    class FailureCase {

        @Test
        void failed_changeParent_notExistsParent() {
            //given
            Category category = createCategory3();

            //when
            assertThatThrownBy(() -> category.changeParentCategory(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("부모 카테고리로 선택한 카테고리가 존재하지 않습니다.");
        }

        @Test
        void failed_changeParent_selfOrChild() {
            //given
            Category category1 = createCategory1();
            Category category2 = createCategory2(category1);

            ReflectionTestUtils.setField(category1, "id", 1L);
            ReflectionTestUtils.setField(category2, "id", 2L);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> category1.changeParentCategory(category1))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("자신 또는 자식을 부모로 설정할 수 없습니다."),
                    () -> assertThatThrownBy(() -> category1.changeParentCategory(category2))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("자신 또는 자식을 부모로 설정할 수 없습니다.")
            );
        }
    }

    private Category createCategory1() {
        return Category.createCategory(null, "가요");
    }

    private Category createCategory2(Category parentCategory) {
        return Category.createCategory(parentCategory, "댄스");
    }

    private Category createCategory3() {
        return Category.createCategory(null, "발라드");
    }
}