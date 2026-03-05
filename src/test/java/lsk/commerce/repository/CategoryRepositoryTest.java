package lsk.commerce.repository;

import jakarta.validation.ConstraintViolationException;
import lsk.commerce.domain.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CategoryRepository.class)
class CategoryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    CategoryRepository categoryRepository;

    @Nested
    class Save {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Category category = Category.createCategory(null, "가요");

                //when
                categoryRepository.save(category);
                em.flush();
                Long categoryId = category.getId();
                em.clear();

                //then
                Category findCategory = em.find(Category.class, categoryId);
                assertAll(
                        () -> assertThat(findCategory)
                                .extracting("id", "name", "parent")
                                .containsExactly(categoryId, "가요", null),
                        () -> assertThat(findCategory.getCategoryProducts()).isEmpty(),
                        () -> assertThat(findCategory.getChild()).isEmpty()
                );
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("wrongNameCategoryProvider")
            void wrongName(Category category, String message) {
                //when
                assertThatThrownBy(() -> categoryRepository.save(category))
                        .isInstanceOf(ConstraintViolationException.class)
                        .hasMessageContaining(message);
            }

            static Stream<Arguments> wrongNameCategoryProvider() {
                return Stream.of(
                        argumentSet("이름 null", Category.createCategory(null, null), "공백일 수 없습니다"),
                        argumentSet("이름 빈 문자열", Category.createCategory(null, ""), "공백일 수 없습니다"),
                        argumentSet("이름 공백", Category.createCategory(null, " "), "공백일 수 없습니다"),
                        argumentSet("이름 20자 초과", Category.createCategory(null, "a".repeat(21)), "크기가 0에서 20 사이여야 합니다")
                );
            }
        }
    }

    abstract class Setup {

        String name;

        @BeforeEach
        void beforeEach() {
            Category parentCategory = Category.createCategory(null, "가요");
            Category childCategory1 = Category.createCategory(parentCategory, "댄스");
            Category childCategory2 = Category.createCategory(parentCategory, "발라드");
            em.persist(parentCategory);
            em.persist(childCategory1);
            em.persist(childCategory2);
            name = parentCategory.getName();
            em.clear();
        }
    }

    @Nested
    class Find extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void withChild() {
                //when
                Optional<Category> findCategory = categoryRepository.findWithChild(name);

                //then
                assertAll(
                        () -> assertThat(findCategory).isPresent(),
                        () -> assertThat(findCategory.get().getChild())
                                .extracting("name")
                                .containsExactlyInAnyOrder("댄스", "발라드")
                );
            }

            @Test
            void all() {
                //when
                List<Category> categories = categoryRepository.findAll();

                //then
                assertThat(categories)
                        .hasSize(3)
                        .extracting("name")
                        .containsExactlyInAnyOrder("가요", "댄스", "발라드");
            }

            @Test
            void byNameSet() {
                //given
                Set<String> nameSet = Set.of("가요", "댄스");

                //when
                List<Category> categories = categoryRepository.findByNameSet(nameSet);

                //then
                assertThat(categories)
                        .hasSize(2)
                        .extracting("name")
                        .containsExactlyInAnyOrder("가요", "댄스");
            }

            @Test
            void byNameSet_ShouldReturnExisting_WhenNamesNotFound() {
                //given
                Set<String> nameSet = Set.of("가요", "록");

                //when
                List<Category> categories = categoryRepository.findByNameSet(nameSet);

                //then
                assertThat(categories)
                        .hasSize(1)
                        .extracting("name")
                        .containsExactlyInAnyOrder("가요");
            }
        }

        @Nested
        class FailureCase {

            @Test
            void withChild_categoryNotFound() {
                //when
                Optional<Category> category = categoryRepository.findWithChild("록");

                //then
                assertThat(category).isEmpty();
            }
        }
    }
}