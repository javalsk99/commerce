package lsk.commerce.integration;

import jakarta.persistence.EntityManager;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.repository.ProductRepository;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDAssertions.tuple;

@Transactional
@SpringBootTest
public class ProductIntegrationTest {

    @Autowired
    EntityManager em;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    @Nested
    class Register {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("상품 이름이 중복돼도 자식 필드가 다르면 다른 상품으로 등록된다")
            void ChildFieldsAreDifferent() {
                //given
                String categoryName = categoryService.create(new CategoryCreateRequest("가요", null));

                productService.register(createRequest("BANG BANG", "IVE", "STARSHIP"), List.of(categoryName));

                em.flush();
                em.clear();

                ProductCreateRequest request = createRequest("BANG BANG", "IVE", "SM");

                System.out.println("================= WHEN START =================");

                //when
                productService.register(request, List.of(categoryName));

                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                List<Product> products = productRepository.findAll();

                then(products)
                        .hasSize(2)
                        .extracting("name", "artist", "studio")
                        .containsExactlyInAnyOrder(tuple("BANG BANG", "IVE", "STARSHIP"), tuple("BANG BANG", "IVE", "SM"));
            }
        }

        @Nested
        class FailureCase {

            @Test
            @DisplayName("상품 이름과 자식 필드가 중복되면 등록할 수 없다")
            void ChildFieldsAreSame() {
                //given
                String categoryName = categoryService.create(new CategoryCreateRequest("가요", null));

                productService.register(createRequest("BANG BANG", "IVE", "STARSHIP"), List.of(categoryName));

                em.flush();
                em.clear();

                ProductCreateRequest request = createRequest("BANG BANG", "IVE", "STARSHIP");

                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> {
                    productService.register(request, List.of(categoryName));
                    em.flush();
                })
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("이미 존재하는 상품입니다");

                System.out.println("================= WHEN END ===================");
            }
        }

        private ProductCreateRequest createRequest(String name, String artist, String studio) {
            return ProductCreateRequest.builder()
                    .name(name)
                    .price(15000)
                    .stockQuantity(10)
                    .dtype("A")
                    .artist(artist)
                    .studio(studio)
                    .build();
        }
    }
}
