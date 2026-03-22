package lsk.commerce.integration;

import jakarta.persistence.EntityManager;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.dto.request.OrderChangeRequest;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.repository.OrderRepository;
import lsk.commerce.repository.PaymentRepository;
import lsk.commerce.repository.ProductRepository;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.MemberService;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.PaymentService;
import lsk.commerce.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@Transactional
@SpringBootTest
public class OrderIntegrationTest {

    @Autowired
    EntityManager em;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    PaymentRepository paymentRepository;

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

    @Nested
    class CreateOrder {

        @Nested
        class FailureCase {

            @Test
            @DisplayName("재고를 초과해서 주문하면 실패한다")
            void exceed() {
                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> {
                    orderService.order(new OrderCreateRequest(memberLoginId, Map.of(albumNumber1, 11)));
                    em.flush();
                })
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("재고가 부족합니다");

                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Product product = productRepository.findByNumber(albumNumber1)
                        .orElseThrow(() -> new AssertionError("상품이 저장되지 않았습니다."));

                then(product.getStockQuantity()).isEqualTo(10);
            }
        }
    }

    @Nested
    class Change {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("주문을 수정하면 상품의 재고가 변경된다")
            void changeOrder() {
                //given
                String orderNumber = orderService.order(new OrderCreateRequest(memberLoginId, Map.of(albumNumber1, 3, albumNumber2, 2)));

                em.flush();
                em.clear();

                OrderChangeRequest request = new OrderChangeRequest(Map.of(albumNumber2, 4));

                System.out.println("================= WHEN START =================");

                //when
                orderService.changeOrder(orderNumber, request);

                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                List<Product> products = productService.findProducts();

                then(products)
                        .extracting("name", "stockQuantity")
                        .containsExactlyInAnyOrder(tuple("BANG BANG", 10), tuple("BLACKHOLE", 6));
            }
        }
    }

    @Nested
    class Cancel {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("주문 시 재고가 감소되고, 주문 취소 시 재고가 복구된다")
            void cancelOrder() {
                System.out.println("============== FIRST WHEN START ==============");

                //when 주문 생성
                String orderNumber = orderService.order(new OrderCreateRequest(memberLoginId, Map.of(albumNumber1, 3, albumNumber2, 2)));

                em.flush();
                em.clear();

                System.out.println("============== FIRST WHEN END ================");

                //then
                List<Product> orderedProducts = productRepository.findAll();

                then(orderedProducts)
                        .extracting("name", "stockQuantity")
                        .containsExactlyInAnyOrder(tuple("BANG BANG", 7), tuple("BLACKHOLE", 8));

                em.flush();
                em.clear();

                System.out.println("============== SECOND WHEN START ==============");

                //when 주문 취소
                orderService.cancelOrder(orderNumber);

                em.flush();
                em.clear();

                System.out.println("============== SECOND WHEN END ================");

                //then
                List<Product> canceledProducts = productRepository.findAll();

                then(canceledProducts)
                        .extracting("stockQuantity")
                        .containsExactlyInAnyOrder(10, 10);
            }
        }
    }

    @Nested
    class Delete {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("주문을 삭제하면 주문 상품, 배송, 결제도 삭제된다")
            void deleteOrder() {
                //given
                String orderNumber = orderService.order(new OrderCreateRequest(memberLoginId, Map.of(albumNumber1, 3, albumNumber2, 2)));
                paymentService.request(orderNumber);

                Order order = orderRepository.findWithAllExceptMember(orderNumber)
                        .orElseThrow(() -> new AssertionError("주문이 저장되지 않았습니다."));
                Long deliveryId = order.getDelivery().getId();
                List<Long> orderProductsId = order.getOrderProducts().stream()
                        .map(OrderProduct::getId)
                        .toList();
                String paymentId = order.getPayment().getPaymentId();

                orderService.cancelOrder(orderNumber);

                em.flush();
                em.clear();

                System.out.println("================= WHEN START =================");

                //when
                orderService.deleteOrder(orderNumber);

                em.flush();
                em.clear();

                System.out.println("================= WHEN END ===================");

                //then
                Optional<Order> deletedOrder = orderRepository.findByOrderNumber(orderNumber);
                Delivery delivery = em.find(Delivery.class, deliveryId);
                Optional<Payment> payment = paymentRepository.findWithOrder(paymentId);
                Boolean result = em.createQuery("select count(*) = 0 from OrderProduct op where op.id in :ids", Boolean.class)
                        .setParameter("ids", orderProductsId)
                        .getSingleResult();

                thenSoftly(softly -> {
                    softly.then(deletedOrder).isEmpty();
                    softly.then(delivery).isNull();
                    softly.then(payment).isEmpty();
                    softly.then(result).isTrue();
                });
            }
        }
    }
}
