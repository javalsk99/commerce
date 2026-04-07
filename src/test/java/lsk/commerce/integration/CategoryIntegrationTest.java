package lsk.commerce.integration;

import jakarta.persistence.EntityManager;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.exception.DuplicateResourceException;
import lsk.commerce.repository.CategoryRepository;
import lsk.commerce.repository.ProductRepository;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@Transactional
@SpringBootTest
public class CategoryIntegrationTest {

    @Autowired
    EntityManager em;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    @Autowired
    CategoryProductService categoryProductService;

    @Nested
    class Create {

        @Nested
        class FailureCase {

            @Test
            @DisplayName("부모 카테고리의 이름이 달라도 같은 이름으로 생성할 수 없다")
            void duplicateCreate() {
                //given
                String parentName1 = categoryService.create(new CategoryCreateRequest("가요", null));
                String parentName2 = categoryService.create(new CategoryCreateRequest("컴퓨터/IT", null));

                categoryService.create(new CategoryCreateRequest("댄스", parentName1));

                em.flush();
                em.clear();

                CategoryCreateRequest request = new CategoryCreateRequest("댄스", parentName2);

                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> {
                    categoryService.create(request);
                    em.flush();
                })
                        .isInstanceOf(DuplicateResourceException.class)
                        .hasMessage("이미 존재하는 카테고리입니다. name: " + "댄스");

                System.out.println("================= WHEN END ===================");
            }
        }
    }

    @Nested
    class Delete {

        @Nested
        class FailureCase {

            @Test
            @DisplayName("상품과 연결된 카테고리는 삭제할 수 없다")
            void hasProduct() {
                //given
                String categoryName = categoryService.create(new CategoryCreateRequest("댄스", null));

                productService.register(createRequest("BANG BANG"), List.of(categoryName));

                em.flush();
                em.clear();

                System.out.println("================= WHEN START =================");

                //when
                thenThrownBy(() -> {
                    categoryService.deleteCategory(categoryName);
                    em.flush();
                })
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("카테고리에 상품이 있어서 삭제할 수 없습니다");

                System.out.println("================= WHEN END ===================");
            }
        }
    }

    @Nested
    class DisconnectProducts {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("카테고리의 모든 상품들의 연결 제거 시, 상품들과 연결이 끊긴다")
            void disconnectAll() {
                //given
                String categoryName = categoryService.create(new CategoryCreateRequest("가요", null));

                productService.register(createRequest("BANG BANG"), List.of(categoryName));
                productService.register(createRequest("BLACKHOLE"), List.of(categoryName));

                em.flush();
                em.clear();

                System.out.println("================= WHEN START =================");

                //when
                categoryProductService.disconnectAll(categoryName);

                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Category category = categoryRepository.findWithChild(categoryName)
                        .orElseThrow(() -> new AssertionError("카테고리가 저장되지 않았습니다"));
                List<Product> products = productRepository.findAll();

                thenSoftly(softly -> {
                    softly.then(category.getCategoryProducts()).isEmpty();
                    softly.then(products)
                            .flatExtracting("categoryProducts")
                            .isEmpty();
                });
            }
        }
    }

    private static ProductCreateRequest createRequest(String name) {
        return ProductCreateRequest.builder()
                .name(name)
                .price(15000)
                .stockQuantity(10)
                .dtype("A")
                .artist("IVE")
                .studio("STARSHIP")
                .build();
    }
}
