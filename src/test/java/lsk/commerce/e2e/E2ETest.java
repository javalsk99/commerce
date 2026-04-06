package lsk.commerce.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import io.jsonwebtoken.Claims;
import io.portone.sdk.server.common.Currency;
import io.portone.sdk.server.common.Customer;
import io.portone.sdk.server.common.PgProvider;
import io.portone.sdk.server.common.PortOneVersion;
import io.portone.sdk.server.common.SelectedChannel;
import io.portone.sdk.server.common.SelectedChannelType;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.PaymentAmount;
import io.portone.sdk.server.payment.PaymentClient;
import jakarta.persistence.EntityManager;
import lsk.commerce.api.portone.CompletePaymentRequest;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Role;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.dto.request.MemberLoginRequest;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.request.OrderProductRequest;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.dto.response.Result;
import lsk.commerce.repository.OrderRepository;
import lsk.commerce.repository.PaymentRepository;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.ProductService;
import lsk.commerce.util.JwtProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class E2ETest {

    @LocalServerPort
    int port;

    @Autowired
    WebTestClient client;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager em;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    @MockitoBean
    PaymentClient portone;

    @Value("${phoneNumber}")
    String phoneNumber;

    @Value("${birth}")
    String birth;

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

    @Test
    @DisplayName("회원 가입부터 결제 완료 후 배송 시작까지의 흐름")
    void mvp() {
        //given
        MemberCreateRequest memberCreateRequest = MemberCreateRequest.builder()
                .name("유저A")
                .loginId("id_A")
                .password("00000000")
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
                .build();

        System.out.println("============== FIRST WHEN START ==============");

        //when & then 회원 가입
        client.post().uri("/members")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(memberCreateRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data").isEqualTo("id_A")
                .jsonPath("$.count").isEqualTo(1)
                .consumeWith(System.out::println);

        System.out.println("============== FIRST WHEN END ================");

        //then
        Member member = em.createQuery(
                        "select m from Member m" +
                                " left join fetch m.orders" +
                                " where m.loginId = :loginId", Member.class)
                .setParameter("loginId", "id_A")
                .getResultList()
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("회원이 저장되지 않았습니다"));

        thenSoftly(softly -> {
            softly.then(member.getId()).isNotNull();
            softly.then(member.getOrders()).isEmpty();
            softly.then(passwordEncoder.matches("00000000", member.getPassword())).isTrue();
            softly.then(member)
                    .extracting("name", "initial", "loginId", "role", "address.city", "address.street", "address.zipcode")
                    .containsExactly("유저A", "ㅇㅈA", "id_A", Role.USER, "Seoul", "Gangnam", "01234");
        });

        //given
        MemberLoginRequest loginRequest = new MemberLoginRequest("id_A", "00000000");

        System.out.println("============== SECOND WHEN START ==============");

        //when & then 로그인
        ResponseCookie responseCookie = client.post().uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(System.out::println)
                .returnResult()
                .getResponseCookies()
                .getFirst("jjwt");

        System.out.println("============== SECOND WHEN END ================");

        //then
        then(responseCookie).isNotNull();
        String token = responseCookie.getValue();

        then(token).isNotNull();
        boolean result = jwtProvider.validateToken(token);
        Claims claims = jwtProvider.extractClaims(token);

        thenSoftly(softly -> {
            softly.then(result).isTrue();
            softly.then(claims.getSubject()).isEqualTo("id_A");
            softly.then(claims.get("role", String.class)).isEqualTo(Role.USER.name());
        });

        //given
        List<OrderProductRequest> orderProductRequestList = createOrderProductRequestList();
        OrderCreateRequest orderCreateRequest = new OrderCreateRequest(orderProductRequestList);

        System.out.println("============== THIRD WHEN START ==============");

        //when & then 주문 생성
        Result<String> body = client.post().uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(orderCreateRequest)
                .cookie("jjwt", token)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<Result<String>>() {
                })
                .consumeWith(System.out::println)
                .returnResult()
                .getResponseBody();

        System.out.println("============== THIRD WHEN END ================");

        //then
        then(body).isNotNull();
        String orderNumber = body.data();
        Order createdOrder = orderRepository.findWithAll(orderNumber)
                .orElseThrow(() -> new AssertionError("주문이 저장되지 않았습니다"));

        thenSoftly(softly -> {
            softly.then(createdOrder.getId()).isNotNull();
            softly.then(createdOrder)
                    .extracting("member.loginId", "delivery.deliveryStatus", "payment", "totalAmount", "orderStatus", "deleted")
                    .containsExactly("id_A", DeliveryStatus.WAITING, null, 139000, OrderStatus.CREATED, false);
            softly.then(createdOrder.getOrderProducts())
                    .extracting("product.name", "quantity", "orderPrice")
                    .containsExactlyInAnyOrder(
                            tuple("BANG BANG", 3, 36000),
                            tuple("BLACKHOLE", 4, 56000),
                            tuple("자바 ORM 표준 JPA 프로그래밍", 2, 20000),
                            tuple("범죄도시2", 3, 27000)
                    );
            softly.then(createdOrder.getOrderProducts())
                    .extracting("product.nameInitial", "product.stockQuantity")
                    .containsExactlyInAnyOrder(
                            tuple("BANG BANG", 7),
                            tuple("BLACKHOLE", 4),
                            tuple("ㅈㅂ ORM ㅍㅈ JPA ㅍㄹㄱㄹㅁ", 10),
                            tuple("ㅂㅈㄷㅅ2", 1)
                    );
        });

        System.out.println("============== FOURTH WHEN START ==============");

        //when & then 결제 요청
        client.post().uri("/payments/orders/{orderNumber}", orderNumber)
                .cookie("jjwt", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.totalAmount").isEqualTo(139000)
                .jsonPath("$.data.paymentStatus").isEqualTo(PaymentStatus.PENDING.name())
                .jsonPath("$.data.orderStatus").isEqualTo(OrderStatus.CREATED.name())
                .jsonPath("$.data.deliveryStatus").isEqualTo(DeliveryStatus.WAITING.name())
                .jsonPath("$.count").isEqualTo(1)
                .consumeWith(System.out::println);

        System.out.println("============== FOURTH WHEN END ================");

        //then
        Order requestedOrder = orderRepository.findWithDeliveryPayment(orderNumber)
                .orElseThrow(() -> new AssertionError("주문이 저장되지 않았습니다"));

        then(requestedOrder)
                .extracting("orderStatus", "delivery.deliveryStatus", "payment.paymentStatus")
                .containsExactly(OrderStatus.CREATED, DeliveryStatus.WAITING, PaymentStatus.PENDING);

        //given
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            BrowserContext context = browser.newContext();
            context.addCookies(Arrays.asList(new Cookie("jjwt", token)
                    .setDomain("localhost")
                    .setPath("/")
            ));
            Page page = context.newPage();

            System.out.println("============== FIFTH WHEN START ==============");

            //when 포트원 서버에 결제 정보 등록
            page.navigate("http://localhost:" + port + "/payments/" + orderNumber);

            System.out.println("============== FIFTH WHEN END ================");

            //then
            thenSoftly(softly -> {
                softly.check(() -> PlaywrightAssertions.assertThat(page).hasTitle("포트원 결제연동"));
                softly.check(() -> PlaywrightAssertions.assertThat(page).hasURL(Pattern.compile("payments/" + orderNumber)));
                softly.check(() -> PlaywrightAssertions.assertThat(page.locator(".orderProductDtoList-product")).hasCount(4));
                softly.check(() -> PlaywrightAssertions.assertThat(page.locator("#totalPriceDisplay")).hasText("139,000원"));
                softly.check(() -> PlaywrightAssertions.assertThat(page.locator("#checkoutButton")).isVisible());
                softly.check(() -> PlaywrightAssertions.assertThat(page.locator("#checkoutButton")).isEnabled());
            });

            String orderName = page.locator("#orderNameDisplay").textContent();

            //given
            FrameLocator paymentLocator = page.locator("#imp-iframe").contentFrame();
            FrameLocator tossLocator = paymentLocator.locator("iframe[name=\"토스페이먼츠 전자결제\"]").contentFrame();

            System.out.println("============== SIXTH WHEN START ==============");

            //when 결제 버튼 클릭
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("결제")).click();

            paymentLocator.getByRole(AriaRole.LINK, new FrameLocator.GetByRoleOptions().setName("토스페이")).click();
            paymentLocator.getByRole(AriaRole.CHECKBOX, new FrameLocator.GetByRoleOptions().setName("[필수] 서비스 이용 약관, 개인정보 처리 동의")).check();
            paymentLocator.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("다음-토스페이 결제")).click();

            tossLocator.getByText("휴대폰번호").click();
            tossLocator.getByRole(AriaRole.TEXTBOX, new FrameLocator.GetByRoleOptions().setName("휴대폰번호")).fill(phoneNumber);
            tossLocator.getByRole(AriaRole.TEXTBOX, new FrameLocator.GetByRoleOptions().setName("생년월일 6자리")).fill(birth);

            System.out.println("============== SIXTH WHEN END ================");

            //then
            thenSoftly(softly -> {
                softly.check(() -> PlaywrightAssertions.assertThat(tossLocator.getByRole(AriaRole.HEADING, new FrameLocator.GetByRoleOptions().setName("토스 앱으로 온 알림을 눌러 결제를 진행해주세요"))).isVisible());
                softly.check(() -> PlaywrightAssertions.assertThat(tossLocator.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("혹시, 알림이 안 왔나요?"))).isVisible());
                softly.check(() -> PlaywrightAssertions.assertThat(tossLocator.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("결제 취소하기"))).isVisible());
            });

            //given
            String paymentId = requestedOrder.getPayment().getPaymentId();
            CompletePaymentRequest completePaymentRequest = new CompletePaymentRequest(paymentId);

            PaidPayment paidPayment = new PaidPayment(paymentId, "transactionId", "merchantId", "storeId", null, new SelectedChannel(SelectedChannelType.Test.INSTANCE, null, null, null, PgProvider.Tosspayments.INSTANCE, "iamporttest_3"), null, PortOneVersion.V2.INSTANCE, null, null, null, Instant.now(), Instant.now(), Instant.now(), orderName, new PaymentAmount(139000L, 0L, null, null, 0L, 139000L, 0L, 0L), Currency.Krw.INSTANCE, new Customer(null, null, null, null, null, null, null, null), null, null, null, null, null, "{\"orderNumber\":\"" + orderNumber + "\"}", null, Instant.now(), null, null, null, null);
            given(portone.getPayment(anyString())).willReturn(CompletableFuture.completedFuture(paidPayment));

            System.out.println("============= SEVENTH WHEN START =============");

            //when 결제 완료
            page.evaluate("""
                    async (id) => {
                              await fetch("/api/payments/complete", {
                                method: "POST",
                                headers: { "Content-Type": "application/json",},
                                credentials: "include",
                                body: JSON.stringify({paymentId: id,}),
                              })
                            }
                    """, paymentId
            );

            System.out.println("============= SEVENTH WHEN END ===============");

            //then 결제 완료 검증
            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        Payment completedPayment = paymentRepository.findWithOrderDelivery(paymentId)
                                .orElseThrow();

                        thenSoftly(softly -> {
                            softly.then(completedPayment)
                                    .extracting("id", "paymentDate")
                                    .isNotNull();
                            softly.then(completedPayment)
                                    .extracting("paymentId", "paymentAmount", "order.orderStatus", "paymentStatus", "order.delivery.deliveryStatus", "deleted")
                                    .containsExactly(paymentId, 139000, OrderStatus.PAID, PaymentStatus.COMPLETED, DeliveryStatus.PREPARING, false);
                        });
                    });

            System.out.println("============== EIGHTH WHEN START ==============");

            //then 배송 시작 검증
            await()
                    .pollDelay(4, TimeUnit.SECONDS)
                    .atMost(7, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        System.out.println("============== EIGHTH WHEN END ================");

                        Order shippedOrder = orderRepository.findWithDelivery(orderNumber)
                                .orElseThrow();

                        then(shippedOrder.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED);
                    });
        }
    }

    private List<OrderProductRequest> createOrderProductRequestList() {
        String categoryName1 = categoryService.create(new CategoryCreateRequest("가요", null));
        String categoryName2 = categoryService.create(new CategoryCreateRequest("컴퓨터/IT", null));
        String categoryName3 = categoryService.create(new CategoryCreateRequest("국내 영화", null));

        String albumNumber1 = productService.register(createAlbumRequest("BANG BANG", 12000, 10), List.of(categoryName1));
        String albumNumber2 = productService.register(createAlbumRequest("BLACKHOLE", 14000, 8), List.of(categoryName1));
        String bookNumber = productService.register(createBookRequest("자바 ORM 표준 JPA 프로그래밍", 10000, 12), List.of(categoryName2));
        String movieNumber1 = productService.register(createMovieRequest("범죄도시2", 9000, 4), List.of(categoryName3));
        productService.register(createMovieRequest("범죄도시3", 11000, 6), List.of(categoryName3));

        OrderProductRequest orderProductRequest1 = new OrderProductRequest(albumNumber1, 3);
        OrderProductRequest orderProductRequest2 = new OrderProductRequest(albumNumber2, 4);
        OrderProductRequest orderProductRequest3 = new OrderProductRequest(bookNumber, 2);
        OrderProductRequest orderProductRequest4 = new OrderProductRequest(movieNumber1, 3);
        return List.of(orderProductRequest1, orderProductRequest2, orderProductRequest3, orderProductRequest4);
    }

    private static ProductCreateRequest createAlbumRequest(String name, Integer price, Integer stockQuantity) {
        return ProductCreateRequest.builder()
                .name(name)
                .price(price)
                .stockQuantity(stockQuantity)
                .dtype("A")
                .artist("IVE")
                .studio("STARSHIP")
                .build();
    }

    private static ProductCreateRequest createBookRequest(String name, int price, int stockQuantity) {
        return ProductCreateRequest.builder()
                .name(name)
                .price(price)
                .stockQuantity(stockQuantity)
                .dtype("B")
                .author("김영한")
                .isbn("9788960777330")
                .build();
    }

    private static ProductCreateRequest createMovieRequest(String name, int price, int stockQuantity) {
        return ProductCreateRequest.builder()
                .name(name)
                .price(price)
                .stockQuantity(stockQuantity)
                .dtype("M")
                .actor("마동석")
                .director("이상용")
                .build();
    }
}
