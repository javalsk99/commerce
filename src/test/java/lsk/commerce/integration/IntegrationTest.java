package lsk.commerce.integration;

import jakarta.persistence.EntityManager;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.request.MemberChangePasswordRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.request.OrderProductRequest;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.dto.response.Result;
import lsk.commerce.repository.MemberRepository;
import lsk.commerce.repository.OrderRepository;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.MemberService;
import lsk.commerce.service.ProductService;
import lsk.commerce.util.JwtProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTest {

    @Autowired
    WebTestClient client;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager em;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    MemberService memberService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    String memberLoginId;
    String token;

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
        Member member = memberService.findMemberByLoginId(memberLoginId);
        token = jwtProvider.createToken(member);
    }

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

    @Nested
    class Authorization {

        @Nested
        class SuccessCase {

            @Test
            @DisplayName("토큰이 있고 본인인 경우 비밀번호 변경은 성공한다")
            void shouldChangePassword_WhenHasToken() {
                //given
                MemberChangePasswordRequest request = new MemberChangePasswordRequest("11111111");

                System.out.println("================= WHEN START =================");

                //when & then
                client.post().uri("/members/{memberLoginId}/password", "id_A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .cookie("jjwt", token)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$.data").isEqualTo("비밀번호가 변경되었습니다")
                        .jsonPath("$.count").isEqualTo(1)
                        .consumeWith(System.out::println);

                System.out.println("================= WHEN END ===================");

                //then
                Member member = memberRepository.findByLoginId("id_A")
                        .orElseThrow(() -> new AssertionError("회원이 저장되지 않았습니다"));

                then(passwordEncoder.matches("11111111", member.getPassword())).isTrue();
            }
        }

        @Nested
        class FailureCase {

            @Test
            @DisplayName("토큰이 없는 경우 401 에러가 발생한다")
            void shouldFailToChange_WhenHasNoToken() {
                //given
                MemberChangePasswordRequest request = new MemberChangePasswordRequest("11111111");

                System.out.println("================= WHEN START =================");

                //when & then
                client.post().uri("/members/{memberLoginId}/password", "id_A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isUnauthorized()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                        .jsonPath("$.message").isEqualTo("로그인을 해야 접근할 수 있습니다")
                        .consumeWith(System.out::println);

                System.out.println("================= WHEN END ===================");
            }

            @Test
            @DisplayName("토큰이 있어도 본인이 아닌 경우 403 에러가 발생한다")
            void shouldFailToChange_WhenMemberIsNotOwner() {
                //given
                MemberChangePasswordRequest request = new MemberChangePasswordRequest("11111111");

                System.out.println("================= WHEN START =================");

                //when & then
                client.post().uri("/members/{memberLoginId}/password", "id_B")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .cookie("jjwt", token)
                        .exchange()
                        .expectStatus().isForbidden()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("NOT_RESOURCE_OWNER")
                        .jsonPath("$.message").isEqualTo("아이디의 주인이 아닙니다")
                        .consumeWith(System.out::println);

                System.out.println("================= WHEN END ===================");
            }

            @Test
            @DisplayName("토큰이 있어도 관리자가 아닌 경우 403 에러가 발생한다")
            void shouldFailToSearch_WhenMemberIsNotAdmin() {
                //given
                MultiValueMap<String, String> cond = new LinkedMultiValueMap<>();
                cond.add("name", "User");
                cond.add("loginId", "id_A");

                System.out.println("================= WHEN START =================");

                //when & then
                client.get().uri(uriBuilder -> uriBuilder
                                .path("/members")
                                .queryParams(cond)
                                .build())
                        .cookie("jjwt", token)
                        .exchange()
                        .expectStatus().isForbidden()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("FORBIDDEN")
                        .jsonPath("$.message").isEqualTo("관리자만 접근할 수 있습니다")
                        .consumeWith(System.out::println);

                System.out.println("================= WHEN END ===================");
            }
        }
    }

    private abstract class SetUp {

        String albumNumber1;
        String albumNumber2;

        @BeforeEach
        void beforeEach() {
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
                //given
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(albumNumber1, 3);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(albumNumber2, 2);
                OrderCreateRequest createRequest = new OrderCreateRequest(List.of(orderProductRequest1, orderProductRequest2));

                System.out.println("============== FIRST WHEN START ==============");

                //when & then 주문 생성
                Result<String> body = client.post().uri("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(createRequest)
                        .cookie("jjwt", token)
                        .exchange()
                        .expectStatus().isCreated()
                        .expectBody(new ParameterizedTypeReference<Result<String>>() {
                        })
                        .consumeWith(System.out::println)
                        .returnResult()
                        .getResponseBody();

                System.out.println("============== FIRST WHEN END ================");

                //then
                String orderNumber = body.data();
                Order createdOrder = orderRepository.findWithAll(orderNumber)
                        .orElseThrow(() -> new AssertionError("주문이 저장되지 않았습니다"));

                thenSoftly(softly -> {
                    softly.then(createdOrder)
                            .extracting("member.loginId", "orderStatus", "delivery.deliveryStatus", "payment", "totalAmount")
                            .containsExactly("id_A", OrderStatus.CREATED, DeliveryStatus.WAITING, null, 75000);
                    softly.then(createdOrder.getOrderProducts())
                            .extracting("product.name", "quantity", "orderPrice")
                            .containsExactlyInAnyOrder(tuple("BANG BANG", 3, 45000), tuple("BLACKHOLE", 2, 30000));
                    softly.then(createdOrder.getOrderProducts())
                            .extracting("product.name", "product.stockQuantity")
                            .containsExactlyInAnyOrder(tuple("BANG BANG", 7), tuple("BLACKHOLE", 8));
                });

                System.out.println("============== SECOND WHEN START ==============");

                //when & then 결제 요청
                client.post().uri("/payments/orders/{orderNumber}", orderNumber)
                        .cookie("jjwt", token)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$.data.totalAmount").isEqualTo(75000)
                        .jsonPath("$.data.paymentStatus").isEqualTo(PaymentStatus.PENDING.name())
                        .jsonPath("$.data.orderStatus").isEqualTo(OrderStatus.CREATED.name())
                        .jsonPath("$.data.deliveryStatus").isEqualTo(DeliveryStatus.WAITING.name())
                        .jsonPath("$.count").isEqualTo(1)
                        .consumeWith(System.out::println);

                System.out.println("============== SECOND WHEN END ================");

                //then
                Order requestOrder = orderRepository.findWithAll(orderNumber)
                        .orElseThrow(() -> new AssertionError("주문이 저장되지 않았습니다"));

                then(requestOrder.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            }
        }
    }
}
