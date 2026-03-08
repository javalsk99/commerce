package lsk.commerce.repository;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.domain.product.Album;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CategoryProductRepository.class)
class CategoryProductRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    CategoryProductRepository categoryProductRepository;

    Category category1;
    Category category2;
    Category category3;
    Category category4;
    Long categoryProductId;

    @BeforeEach
    void beforeEach() {
        category1 = Category.createCategory(null, "가요");
        category2 = Category.createCategory(category1, "댄스");
        category3 = Category.createCategory(category1, "발라드");
        category4 = Category.createCategory(category1, "OST");
        em.persist(category1);
        em.persist(category2);
        em.persist(category3);
        em.persist(category4);

        Album album1 = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build();
        Album album2 = Album.builder().name("타임 캡슐").price(15000).stockQuantity(10).artist("다비치").studio("씨에이엠위더스").build();
        em.persist(album1);
        em.persist(album2);
        album1.connectCategory(category2);
        album2.connectCategory(category3);

        em.flush();
        categoryProductId = album1.getCategoryProducts().getFirst().getId();
        em.clear();
    }

    @Nested
    class Find {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                System.out.println("================= WHEN START =================");

                //when
                List<CategoryProduct> findCategoryProducts = categoryProductRepository.findAllWithProductByCategory(category1);

                System.out.println("================= WHEN END ===================");

                //then
                assertAll(
                        () -> assertThat(Hibernate.isInitialized(findCategoryProducts.getFirst().getProduct())).isTrue(),
                        () -> assertThat(Hibernate.isInitialized(findCategoryProducts.getFirst().getCategory())).isFalse(),
                        () -> assertThat(findCategoryProducts)
                                .hasSize(2)
                                .extracting("product.name")
                                .containsExactlyInAnyOrder("BANG BANG", "타임 캡슐")
                );
            }

            @Test
            void categoryProductIsNull() {
                System.out.println("================= WHEN START =================");

                //when
                List<CategoryProduct> findCategoryProducts = categoryProductRepository.findAllWithProductByCategory(category4);

                System.out.println("================= WHEN END ===================");

                //then
                assertThat(findCategoryProducts).isEmpty();
            }
        }
    }

    @Nested
    class Delete {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                CategoryProduct findCategoryProduct = em.find(CategoryProduct.class, categoryProductId);

                System.out.println("================= WHEN START =================");

                //when
                categoryProductRepository.delete(findCategoryProduct);
                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                CategoryProduct deletedCategoryProduct = em.find(CategoryProduct.class, categoryProductId);
                assertThat(deletedCategoryProduct).isNull();
            }
        }
    }
}