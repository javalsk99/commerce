package lsk.commerce.service;

import jakarta.persistence.EntityManager;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.event.PaymentCompletedEvent;
import org.hibernate.TransientObjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    EntityManager em;

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
    @Autowired
    DeliveryService deliveryService;

    String memberLoginId;
    Category category1;
    Category category2;
    Category category3;
    Album album;
    Book book;
    Movie movie;
    String orderNumber;

    @BeforeEach
    void beforeEach() {
        memberLoginId = createMember1();

        category1 = createCategory1();
        category2 = createCategory2();
        category3 = createCategory3();

        album = createAlbum();
        book = createBook();
        movie = createMovie();
        productService.register(album, List.of(category1));
        productService.register(book, List.of(category2));
        productService.register(movie, List.of(category3));

        orderNumber = orderService.order(memberLoginId, Map.of(album.getName(), 3, book.getName(), 2, movie.getName(), 5));
    }

    @Test
    void order() {
        //given
        Product findAlbum = productService.findProductByName(album.getName());
        Product findBook = productService.findProductByName(book.getName());
        Product findMovie = productService.findProductByName(movie.getName());

        //when
        String number = orderService.order(memberLoginId, Map.of(findAlbum.getName(), 3, findBook.getName(), 5, findMovie.getName(), 2));

        //then
        Order findOrder = orderService.findOrderWithAll(number);
        assertThat(findOrder.getOrderProducts().size()).isEqualTo(3);
        assertThat(findOrder.getTotalAmount()).isEqualTo(274000);

        assertThat(findAlbum.getStockQuantity()).isEqualTo(14);
        assertThat(findBook.getStockQuantity()).isEqualTo(3);
        assertThat(findMovie.getStockQuantity()).isEqualTo(8);
        assertThat(findOrder.getOrderProducts())
                .extracting("product.name", "count", "orderPrice")
                .contains(tuple(findAlbum.getName(), 3, 45000), tuple(findBook.getName(), 5, 215000), tuple(findMovie.getName(), 2, 14000));

        assertThat(findOrder.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.WAITING);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("memberProductCountProvider")
    void failed_order(String memberLoginId, String productName, Integer count, String reason) {
        //given
        Map<String, Integer> productMap = new HashMap<>();
        productMap.put(productName, count);

        //when
        assertThrows(IllegalArgumentException.class, () ->
                orderService.order(memberLoginId, productMap));
    }

    @Test
    void find() {
        //when
        Order findOrder = orderService.findOrderWithAll(orderNumber);

        //then
        assertThat(findOrder.getMember().getLoginId()).isEqualTo("id_A");
        assertThat(findOrder.getOrderProducts())
                .extracting("product.name", "count")
                .containsExactlyInAnyOrder(tuple("하얀 그리움", 3), tuple("자바 ORM 표준 JPA 프로그래밍", 2), tuple("굿뉴스", 5));
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("orderNumberProvider")
    void failed_find(String number, String reason) {
        //when
        assertThrows(IllegalArgumentException.class, () ->
                orderService.findOrderWithAll(number));
    }

    @Test
    void update() {
        //when
        orderService.updateOrder(orderNumber, Map.of(album.getName(), 4, movie.getName(), 2));

        //then
        Order findOrder = orderService.findOrderWithAll(orderNumber);
        assertThat(findOrder.getOrderProducts().size()).isEqualTo(2);
        assertThat(findOrder.getTotalAmount()).isEqualTo(74000);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("productMapProvider")
    void failed_update(String productName, Integer count, String reason) {
        //given
        Map<String, Integer> newProductMap = new HashMap<>();
        newProductMap.put(productName, count);

        //when
        assertThrows(IllegalArgumentException.class, () ->
                orderService.updateOrder(orderNumber, newProductMap));
    }

    @Test
    void cancel() {
        //when
        orderService.cancelOrder(orderNumber);

        //then
        Order findOrder = orderService.findOrderWithDeliveryPayment(orderNumber);
        assertThat(findOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(findOrder.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED);
    }

    @Test
    void failed_cancel_paidOrder() {
        //given
        Order order = paymentService.request(orderNumber);
        paymentService.completePayment(order.getPayment().getPaymentId(), LocalDateTime.now());

        //when
        assertThrows(IllegalStateException.class, () ->
                orderService.cancelOrder(orderNumber));
    }

    @Test
    void failed_cancel_alreadyCancel() {
        //given
        orderService.cancelOrder(orderNumber);

        //when
        assertThrows(IllegalStateException.class, () ->
                orderService.cancelOrder(orderNumber));
    }

    @Test
    void delete_canceledOrder() {
        //given
        orderService.cancelOrder(orderNumber);

        //when
        orderService.deleteOrder(orderNumber);

        //then
        assertThrows(IllegalArgumentException.class, () ->
                orderService.findOrderWithDeliveryPayment(orderNumber));
    }

    @Test
    void delete_deliveredOrder() {
        //given
        Order order = paymentService.request(orderNumber);
        paymentService.completePayment(order.getPayment().getPaymentId(), LocalDateTime.now());

        deliveryService.startDelivery(orderNumber);
        deliveryService.completeDelivery(orderNumber);

        //when
        orderService.deleteOrder(orderNumber);

        //then
        assertThrows(IllegalArgumentException.class, () ->
                orderService.findOrderWithDeliveryPayment(orderNumber));
    }

    @Test
    void failed_delete_uncanceled() {
        //when
        assertThrows(IllegalStateException.class, () ->
                orderService.deleteOrder(orderNumber));
    }

    @Test
    void failed_delete_alreadyDeleted() {
        //given
        orderService.cancelOrder(orderNumber);
        orderService.deleteOrder(orderNumber);

        //when
        assertThrows(IllegalArgumentException.class, () ->
                orderService.deleteOrder(orderNumber));
    }

/*
    @Test
    void paid_order() {
        //given
        Long orderId = createOrder();
        Order findOrder = orderService.findOrder(orderId);

        Payment.requestPayment(findOrder);
        findOrder.getPayment().testCompleted();
        findOrder.testPaid();

        //when
        findOrder.getDelivery().startShipping(findOrder);

        //then
        assertThat(findOrder.getDelivery().getDeliveryStatus()).isEqualTo(PREPARING);
    }
*/

    private String createMember1() {
        Member member = new Member("userA", "id_A", "00000000", "Seoul", "Gangnam", "01234");
        return memberService.join(member);
    }

    private String createMember2() {
        Member member = new Member("userB", "id_B", "11111111", "Seoul", "Gangbuk", "01235");
        return memberService.join(member);
    }

    private Category createCategory1() {
        return categoryService.findCategoryByName(categoryService.create("가요", null));
    }

    private Category createCategory2() {
        return categoryService.findCategoryByName(categoryService.create("컴퓨터/IT", null));
    }

    private Category createCategory3() {
        return categoryService.findCategoryByName(categoryService.create("Comedy", null));
    }

    private Album createAlbum() {
        return new Album("하얀 그리움", 15000, 20, "fromis_9", "ASND");
    }

    private Book createBook() {
        return new Book("자바 ORM 표준 JPA 프로그래밍", 43000, 10, "김영한", "9788960777330");
    }

    private Movie createMovie() {
        return new Movie("굿뉴스", 7000, 15, "설경구", "변성현");
    }

    static Stream<Arguments> memberProductCountProvider() {
        return Stream.of(
                arguments(null, "하얀 그리움", 3, "회원 아이디 null"),
                arguments("", "하얀 그리움", 3, "회원 아이디 빈 문자열"),
                arguments(" ", "하얀 그리움", 3, "회원 아이디 공백"),
                arguments("id_B", "하얀 그리움", 3, "존재하지 않는 회원 아이디"),
                arguments("id_A", null, 3, "상품 이름 null"),
                arguments("id_A", "", 3, "상품 이름 빈 문자열"),
                arguments("id_A", " ", 3, "상품 이름 공백"),
                arguments("id_A", "BANG BANG", 3, "존재하지 않는 상품 이름"),
                arguments("id_A", "하얀 그리움", null, "주문 수량 null"),
                arguments(null, "하얀 그리움", 18, "재고 수량 초과")
        );
    }

    static Stream<Arguments> orderNumberProvider() {
        return Stream.of(
                arguments(null, "주문 번호 null"),
                arguments("", "주문 번호 빈 문자열"),
                arguments(" ", "주문 번호 공백"),
                arguments("12345", "존재하지 않는 주문 번호")
        );
    }

    static Stream<Arguments> productMapProvider() {
        return Stream.of(
                arguments(null, 2, "상품 이름 null"),
                arguments("", 2, "상품 이름 빈 문자열"),
                arguments(" ", 2, "상품 이름 공백"),
                arguments("BANG BANG", 2, "존재하지 않는 상품"),
                arguments("하얀 그리움", null, "주문 수량 null"),
                arguments("하얀 그리움", 21, "재고 수량 초과")
        );
    }
}