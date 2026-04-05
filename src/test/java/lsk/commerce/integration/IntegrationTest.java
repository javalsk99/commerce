package lsk.commerce.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

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
            void shouldChangePassword_WhenHasToken() throws Exception {
                //given
                MemberChangePasswordRequest request = new MemberChangePasswordRequest("11111111");
                String json = objectMapper.writeValueAsString(request);

                System.out.println("================= WHEN START =================");

                //when & then
                mvc.perform(post("/members/{memberLoginId}/password", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                                .cookie(new Cookie("jjwt", token)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("비밀번호가 변경되었습니다"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

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
            void shouldFailToChange_WhenHasNoToken() throws Exception {
                //given
                MemberChangePasswordRequest request = new MemberChangePasswordRequest("11111111");
                String json = objectMapper.writeValueAsString(request);

                System.out.println("================= WHEN START =================");

                //when & then
                mvc.perform(post("/members/{memberLoginId}/password", "id_A")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                        .andExpect(jsonPath("$.message").value("로그인을 해야 접근할 수 있습니다"))
                        .andDo(print());

                System.out.println("================= WHEN END ===================");
            }

            @Test
            @DisplayName("토큰이 있어도 본인이 아닌 경우 403 에러가 발생한다")
            void shouldFailToChange_WhenMemberIsNotOwner() throws Exception {
                //given
                MemberChangePasswordRequest request = new MemberChangePasswordRequest("11111111");
                String json = objectMapper.writeValueAsString(request);

                System.out.println("================= WHEN START =================");

                //when & then
                mvc.perform(post("/members/{memberLoginId}/password", "id_B")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                                .cookie(new Cookie("jjwt", token)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value("NOT_RESOURCE_OWNER"))
                        .andExpect(jsonPath("$.message").value("아이디의 주인이 아닙니다"))
                        .andDo(print());

                System.out.println("================= WHEN END ===================");
            }

            @Test
            @DisplayName("토큰이 있어도 관리자가 아닌 경우 403 에러가 발생한다")
            void shouldFailToSearch_WhenMemberIsNotAdmin() throws Exception {
                //given
                MultiValueMap<String, String> cond = new LinkedMultiValueMap<>();
                cond.add("name", "User");
                cond.add("loginId", "id_A");

                System.out.println("================= WHEN START =================");

                //when & then
                mvc.perform(get("/members")
                                .queryParams(cond)
                                .cookie(new Cookie("jjwt", token)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                        .andExpect(jsonPath("$.message").value("관리자만 접근할 수 있습니다"))
                        .andDo(print());

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
            void CreateOrderAndRequestPayment() throws Exception {
                //given
                OrderProductRequest orderProductRequest1 = new OrderProductRequest(albumNumber1, 3);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(albumNumber2, 2);
                OrderCreateRequest createRequest = new OrderCreateRequest(List.of(orderProductRequest1, orderProductRequest2));
                String json = objectMapper.writeValueAsString(createRequest);

                System.out.println("============== FIRST WHEN START ==============");

                //when & then 주문 생성
                String body = mvc.perform(post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json)
                                .cookie(new Cookie("jjwt", token)))
                        .andExpect(status().isCreated())
                        .andDo(print())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

                System.out.println("============== FIRST WHEN END ================");

                //then
                Result<String> result = objectMapper.readValue(body, new TypeReference<Result<String>>() {
                });
                String orderNumber = result.data();
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
                mvc.perform(post("/payments/orders/{orderNumber}", orderNumber)
                                .cookie(new Cookie("jjwt", token)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalAmount").value(75000))
                        .andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.PENDING.name()))
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.WAITING.name()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                System.out.println("============== SECOND WHEN END ================");

                //then
                Order requestOrder = orderRepository.findWithAll(orderNumber)
                        .orElseThrow(() -> new AssertionError("주문이 저장되지 않았습니다"));

                then(requestOrder.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            }
        }
    }
}
