package lsk.commerce.query;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.product.Album;
import lsk.commerce.query.dto.CategoryProductQueryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.tuple;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CategoryProductQueryRepository.class)
class CategoryProductQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    CategoryProductQueryRepository categoryProductQueryRepository;

    String productNumber1;
    String productNumber2;

    @BeforeEach
    void beforeEach() {
        Category category = Category.createCategory(null, "가요");
        em.persistAndFlush(category);

        Album album1 = createAlbum("BANG BANG");
        Album album2 = createAlbum("BLACKHOLE");
        productNumber1 = album1.getProductNumber();
        productNumber2 = album2.getProductNumber();

        em.flush();

        album1.connectCategory(category);
        album2.connectCategory(category);

        em.flush();
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
                List<CategoryProductQueryDto> categoryProductQueryDtoList = categoryProductQueryRepository.findCategoryProductsByCategoryName("가요");

                System.out.println("================= WHEN END ===================");

                //then
                then(categoryProductQueryDtoList)
                        .extracting("productName", "productNumber")
                        .containsExactlyInAnyOrder(tuple("BANG BANG", productNumber1), tuple("BLACKHOLE", productNumber2));
            }
        }
    }

    private Album createAlbum(String name) {
        Album album = Album.builder()
                .name(name)
                .price(15000)
                .stockQuantity(10)
                .artist("IVE")
                .studio("STARSHIP")
                .build();
        em.persist(album);
        return album;
    }
}