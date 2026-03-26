package lsk.commerce.e2e;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import lsk.commerce.api.portone.CompletePaymentRequest;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Grade;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.request.MemberCreateRequest;
import lsk.commerce.dto.request.MemberLoginRequest;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.dto.response.Result;
import lsk.commerce.repository.CategoryRepository;
import lsk.commerce.repository.OrderRepository;
import lsk.commerce.repository.ProductRepository;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.ProductService;
import lsk.commerce.util.JwtProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class E2ETest {

    @Autowired
    WebTestClient client;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    EntityManager em;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

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
    @DisplayName("회원 가입부터 결제 완료까지의 흐름")
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
                    .extracting("name", "initial", "loginId", "grade", "address.city", "address.street", "address.zipcode")
                    .containsExactly("유저A", "ㅇㅈA", "id_A", Grade.USER, "Seoul", "Gangnam", "01234");
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
            softly.then(claims.get("grade", String.class)).isEqualTo(Grade.USER.name());
        });

        //given
        Map<String, Integer> productMap = createProductMap();
        OrderCreateRequest orderCreateRequest = new OrderCreateRequest(productMap);

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
                    .extracting("product.name", "count", "orderPrice")
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
    }

    private Map<String, Integer> createProductMap() {
        String categoryName1 = categoryService.create(new CategoryCreateRequest("가요", null));
        String categoryName2 = categoryService.create(new CategoryCreateRequest("컴퓨터/IT", null));
        String categoryName3 = categoryService.create(new CategoryCreateRequest("국내 영화", null));

        String albumNumber1 = productService.register(createAlbumRequest("BANG BANG", 12000, 10), List.of(categoryName1));
        String albumNumber2 = productService.register(createAlbumRequest("BLACKHOLE", 14000, 8), List.of(categoryName1));
        String bookNumber = productService.register(createBookRequest("자바 ORM 표준 JPA 프로그래밍", 10000, 12), List.of(categoryName2));
        String movieNumber1 = productService.register(createMovieRequest("범죄도시2", 9000, 4), List.of(categoryName3));
        productService.register(createMovieRequest("범죄도시3", 11000, 6), List.of(categoryName3));

        return Map.of(albumNumber1, 3, albumNumber2, 4, bookNumber, 2, movieNumber1, 3);
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
