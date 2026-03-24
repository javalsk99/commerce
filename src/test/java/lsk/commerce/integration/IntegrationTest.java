package lsk.commerce.integration;

import jakarta.persistence.EntityManager;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.repository.OrderRepository;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.MemberService;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.PaymentService;
import lsk.commerce.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@SpringBootTest
public class IntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager em;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    MemberService memberService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    @Autowired
    OrderService orderService;

    @Autowired
    PaymentService paymentService;

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("DELETE FROM member");
        jdbcTemplate.execute("DELETE FROM category");
        jdbcTemplate.execute("DELETE FROM category_product");
        jdbcTemplate.execute("DELETE FROM product");
        jdbcTemplate.execute("DELETE FROM payment");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM order_product");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        em.clear();
    }

    private abstract class SetUp {

        String memberLoginId;
        String albumNumber1;
        String albumNumber2;

        @BeforeEach
        void beforeEach() {
            memberLoginId = memberService.join(MemberCreateRequest.builder()
                    .name("UserA")
                    .loginId("id_A")
                    .password("00000000")
                    .city("Seoul")
                    .street("Gangnam")
                    .zipcode("01234")
                    .build());
            String parentCategoryName = categoryService.create(new CategoryCreateRequest("가요", null));
            String childCategoryName = categoryService.create(new CategoryCreateRequest("댄스", parentCategoryName));
            albumNumber1 = productService.register(ProductCreateRequest.builder()
                    .name("BANG BANG")
                    .price(15000)
                    .stockQuantity(10)
                    .dtype("A")
                    .artist("IVE")
                    .studio("STARSHIP")
                    .build(), List.of(childCategoryName));
            albumNumber2 = productService.register(ProductCreateRequest.builder()
                    .name("BLACKHOLE")
                    .price(15000)
                    .stockQuantity(10)
                    .dtype("A")
                    .artist("IVE")
                    .studio("STARSHIP")
                    .build(), List.of(childCategoryName));
        }
    }

    @Nested
    class OrderPaymentIntegration extends SetUp {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("주문 생성 후 결제 요청 흐름")
            void CreateOrderAndRequestPayment() {
                System.out.println("============== FIRST WHEN START ==============");

                //when 주문 생성
                String orderNumber = orderService.order(new OrderCreateRequest(memberLoginId, Map.of(albumNumber1, 3, albumNumber2, 2)));

                System.out.println("============== FIRST WHEN END ================");

                //then
                Order createdOrder = orderRepository.findWithAll(orderNumber)
                        .orElseThrow(() -> new AssertionError("주문이 저장되지 않았습니다"));

                thenSoftly(softly -> {
                    softly.then(createdOrder)
                            .extracting("member.loginId", "delivery.deliveryStatus", "payment", "totalAmount", "orderStatus", "deleted")
                            .containsExactly("id_A", DeliveryStatus.WAITING, null, 75000, OrderStatus.CREATED, false);
                    softly.then(createdOrder.getOrderProducts())
                            .extracting("product.name", "product.stockQuantity")
                            .containsExactlyInAnyOrder(tuple("BANG BANG", 7), tuple("BLACKHOLE", 8));
                });

                System.out.println("============== SECOND WHEN START ==============");

                //when 결제 요청
                paymentService.request(orderNumber);

                System.out.println("============== SECOND WHEN END ================");

                //then
                Order requestedOrder = orderRepository.findWithAll(orderNumber)
                        .orElseThrow(() -> new AssertionError("주문이 저장되지 않았습니다"));

                then(requestedOrder)
                        .extracting("member.loginId", "delivery.deliveryStatus", "payment.paymentStatus", "totalAmount", "orderStatus", "deleted")
                        .containsExactly("id_A", DeliveryStatus.WAITING, PaymentStatus.PENDING, 75000, OrderStatus.CREATED, false);
            }
        }
    }
}
