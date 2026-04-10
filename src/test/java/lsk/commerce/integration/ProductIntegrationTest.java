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

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@Transactional
@SpringBootTest
public class ProductIntegrationTest {

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
    class Register {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("상품 등록 시, 카테고리와 연결된다")
            void basic() {
                //given
                String categoryName = categoryService.create(new CategoryCreateRequest("가요", null));

                em.flush();
                em.clear();

                ProductCreateRequest request = createRequest("BANG BANG", "IVE", "STARSHIP");

                System.out.println("================= WHEN START =================");

                //when
                String productNumber = productService.register(request, List.of(categoryName));

                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Product product = productRepository.findWithCategoryProductCategory(productNumber)
                        .orElseThrow(() -> new AssertionError("상품이 저장되지 않았습니다"));
                Category category = categoryRepository.findWithChild(categoryName)
                        .orElseThrow(() -> new AssertionError("카테고리가 저장되지 않았습니다"));

                thenSoftly(softly -> {
                    softly.then(product.getCategoryProducts())
                            .extracting("category.name")
                            .containsExactly("가요");
                    softly.then(category.getCategoryProducts())
                            .extracting("product.name")
                            .containsExactly("BANG BANG");
                });
            }

            @Test
            @DisplayName("상품 이름이 중복되어도 자식 필드가 다르면 다른 상품으로 등록된다")
            void childFieldsAreDifferent() {
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
            void childFieldsAreSame() {
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
                        .isInstanceOf(DuplicateResourceException.class)
                        .hasMessage("이미 존재하는 상품입니다. name: " + "BANG BANG");

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

    @Nested
    class ChangeConnectCategory {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("상품의 카테고리 제거 및 연결 시, 카테고리와의 연결이 변경된다")
            void disconnectAndConnect() {
                //given
                String categoryName = categoryService.create(new CategoryCreateRequest("가요", null));

                String productNumber = productService.register(createRequest(), List.of(categoryName));

                em.flush();
                em.clear();

                System.out.println("============== FIRST WHEN START ==============");

                //when 상품의 카테고리 제거
                categoryProductService.disconnect(categoryName, productNumber);

                em.flush();
                em.clear();

                System.out.println("============== FIRST WHEN END ================");

                //then
                Product disconnectedProduct = productRepository.findWithCategoryProductCategory(productNumber)
                        .orElseThrow(() -> new AssertionError("상품이 저장되지 않았습니다"));
                Category disconnectedCategory = categoryRepository.findWithChild(categoryName)
                        .orElseThrow(() -> new AssertionError("카테고리가 저장되지 않았습니다"));

                thenSoftly(softly -> {
                    softly.then(disconnectedProduct.getCategoryProducts()).isEmpty();
                    softly.then(disconnectedCategory.getCategoryProducts()).isEmpty();
                });

                em.flush();
                em.clear();

                System.out.println("============== SECOND WHEN START ==============");

                //when 상품과 카테고리 연결
                categoryProductService.connect(productNumber, categoryName);

                em.flush();
                em.clear();

                System.out.println("============== SECOND WHEN END ================");

                //then
                Product connectedProduct = productRepository.findWithCategoryProductCategory(productNumber)
                        .orElseThrow(() -> new AssertionError("상품이 저장되지 않았습니다"));
                Category connectedCategory = categoryRepository.findWithChild(categoryName)
                        .orElseThrow(() -> new AssertionError("카테고리가 저장되지 않았습니다"));

                thenSoftly(softly -> {
                    softly.then(connectedProduct.getCategoryProducts())
                            .extracting("category.name")
                            .containsExactly("가요");
                    softly.then(connectedCategory.getCategoryProducts())
                            .extracting("product.name")
                            .containsExactly("BANG BANG");
                });
            }

            private ProductCreateRequest createRequest() {
                return ProductCreateRequest.builder()
                        .name("BANG BANG")
                        .price(15000)
                        .stockQuantity(10)
                        .dtype("A")
                        .artist("IVE")
                        .studio("STARSHIP")
                        .build();
            }
        }
    }
}
