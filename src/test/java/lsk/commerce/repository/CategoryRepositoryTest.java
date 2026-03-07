package lsk.commerce.repository;

import jakarta.validation.ConstraintViolationException;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.product.Album;
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

                System.out.println("================= WHEN START =================");

                //when
                categoryRepository.save(category);
                em.flush();
                Long categoryId = category.getId();
                em.clear();

                System.out.println("================= WHEN END ===================");

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

            @Test
            void existsLoginId() {
                //given
                Category parentCategory = Category.createCategory(null, "가요");
                Category category1 = Category.createCategory(null, "댄스");
                em.persist(parentCategory);
                em.persist(category1);
                em.flush();
                em.clear();

                Category category2 = Category.createCategory(parentCategory, "댄스");

                System.out.println("================= WHEN START =================");

                //when
                assertThatThrownBy(() -> categoryRepository.save(category2))
                        .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class)
                        .hasMessageContaining("Duplicate entry");

                System.out.println("================= WHEN END ===================");
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

        Long categoryId1;
        Long categoryId2;
        Long categoryId3;
        String name1;
        String name2;
        String name3;
        Album album1;
        Album album2;

        @BeforeEach
        void beforeEach() {
            Category parentCategory = Category.createCategory(null, "가요");
            Category childCategory1 = Category.createCategory(parentCategory, "댄스");
            Category childCategory2 = Category.createCategory(parentCategory, "발라드");
            em.persist(parentCategory);
            em.persist(childCategory1);
            em.persist(childCategory2);
            categoryId1 = parentCategory.getId();
            categoryId2 = childCategory1.getId();
            categoryId3 = childCategory2.getId();
            name1 = parentCategory.getName();
            name2 = childCategory1.getName();
            name3 = childCategory2.getName();

            album1 = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build();
            album2 = Album.builder().name("타임 캡슐").price(15000).stockQuantity(10).artist("다비치").studio("씨에이엠위더스").build();
            em.persist(album1);
            em.persist(album2);
            album1.connectCategory(childCategory1);
            album2.connectCategory(childCategory2);

            em.flush();
            em.clear();
        }
    }

    @Nested
    class Find extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void withChild() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<Category> findCategory = categoryRepository.findWithChild(name1);

                System.out.println("================= WHEN END ===================");

                //then
                assertAll(
                        () -> assertThat(findCategory).isPresent(),
                        () -> assertThat(findCategory.get().getChild())
                                .extracting("name")
                                .containsExactlyInAnyOrder("댄스", "발라드"),
                        () -> assertThat(findCategory.get().getCategoryProducts())
                                .hasSize(2)
                                .extracting("product.name")
                                .containsExactlyInAnyOrder("BANG BANG", "타임 캡슐")
                );
            }

            @Test
            void all() {
                System.out.println("================= WHEN START =================");

                //when
                List<Category> categories = categoryRepository.findAll();

                System.out.println("================= WHEN END ===================");

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

                System.out.println("================= WHEN START =================");

                //when
                List<Category> categories = categoryRepository.findByNameSet(nameSet);

                System.out.println("================= WHEN END ===================");

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

                System.out.println("================= WHEN START =================");

                //when
                List<Category> categories = categoryRepository.findByNameSet(nameSet);

                System.out.println("================= WHEN END ===================");

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
                System.out.println("================= WHEN START =================");

                //when
                Optional<Category> category = categoryRepository.findWithChild("록");

                System.out.println("================= WHEN END ===================");

                //then
                assertThat(category).isEmpty();
            }
        }
    }

    @Nested
    class Delete extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Category category = em.find(Category.class, categoryId3);
                deleteCategoryProducts(category);

                System.out.println("================= WHEN START =================");

                //when
                categoryRepository.delete(category);
                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Category findCategory = em.find(Category.class, categoryId3);
                assertThat(findCategory).isNull();
            }

            private void deleteCategoryProducts(Category category) {
                List<CategoryProduct> categoryProducts = category.getCategoryProducts();
                for (CategoryProduct categoryProduct : categoryProducts) {
                    em.remove(categoryProduct);
                }
                em.flush();
            }
        }

        @Nested
        class FailureCase {

            @Test
            void hasChild() {
                Category findCategory = em.find(Category.class, categoryId1);

                System.out.println("================= WHEN START =================");

                //when
                assertThatThrownBy(() -> {
                    categoryRepository.delete(findCategory);
                    em.flush();
                })
                        .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class)
                        .hasMessageContaining("foreign key")
                        .hasMessageContaining("parent_id");

                System.out.println("================= WHEN END ===================");
            }

            @Test
            void hasCategoryProduct() {
                Category findCategory = em.find(Category.class, categoryId3);

                System.out.println("================= WHEN START =================");

                //when
                assertThatThrownBy(() -> {
                    categoryRepository.delete(findCategory);
                    em.flush();
                })
                        .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class)
                        .hasMessageContaining("foreign key")
                        .hasMessageContaining("category_product");

                System.out.println("================= WHEN END ===================");
            }
        }
    }

    @Nested
    class ExistsByCategoryNames extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void withoutParent() {
                System.out.println("================= WHEN START =================");

                //when
                List<Category> categories = categoryRepository.existsByCategoryNames(name2, null);

                System.out.println("================= WHEN END ===================");

                //then
                assertThat(categories)
                        .extracting("name")
                        .containsExactly("댄스");
            }

            @Test
            void withParent() {
                System.out.println("================= WHEN START =================");

                //when
                List<Category> categories = categoryRepository.existsByCategoryNames(name2, name1);

                System.out.println("================= WHEN END ===================");

                //then
                assertThat(categories)
                        .hasSize(2)
                        .extracting("name")
                        .containsExactlyInAnyOrder("가요", "댄스");
            }

            @Test
            void shouldReturnExisting_WhenCategoryNameDoesNotExists() {
                System.out.println("================= WHEN START =================");

                //when
                List<Category> categories = categoryRepository.existsByCategoryNames("록", name1);

                System.out.println("================= WHEN END ===================");

                //then
                assertThat(categories)
                        .extracting("name")
                        .containsExactly("가요");
            }
        }
    }
}