package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.MemberRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
@Transactional
class PaymentServiceTest {

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
    Category category1;
    Category category2;
    Category category3;
    Album album;
    Book book;
    Movie movie;
    String orderNumber1;
    String orderNumber2;
    String paymentId;

    @BeforeEach
    void beforeEach() {
        memberLoginId = createMember();

        category1 = createCategory1();
        category2 = createCategory2();
        category3 = createCategory3();

        album = createAlbum();
        book = createBook();
        movie = createMovie();
        productService.register(album, List.of(category1));
        productService.register(book, List.of(category2));
        productService.register(movie, List.of(category3));

        orderNumber1 = orderService.order(memberLoginId, Map.of(album.getName(), 3, book.getName(), 2, movie.getName(), 5));
        orderNumber2 = orderService.order(memberLoginId, Map.of(album.getName(), 3, book.getName(), 2, movie.getName(), 5));

        Order order = paymentService.request(orderNumber1);
        paymentId = order.getPayment().getPaymentId();
    }

    @Test
    void request() {
        //when
        Order order = paymentService.request(orderNumber2);

        //then
        Payment payment = order.getPayment();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentAmount()).isEqualTo(order.getTotalAmount());
        assertThat(payment.getPaymentDate()).isNull();
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("orderNumberProvider")
    void failed_request_wrongOrderNumber(String number, String reason) {
        //when
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.request(number));
    }

    @Test
    void failed_request_alreadyRequest() {
        //when
        assertThrows(IllegalStateException.class, () ->
                paymentService.request(orderNumber1));
    }

    @Test
    void find() {
        //when
        Payment findPayment = paymentService.findPaymentByPaymentId(paymentId);

        //then
        assertThat(findPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("paymentIdProvider")
    void failed_find_wrongPaymentId(String id, String reason) {
        //when
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.findPaymentByPaymentId(id));
    }

    @Test
    void failedPayment() {
        //when
        Payment failedPayment = paymentService.failedPayment(paymentId);

        //then
        assertThat(failedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failedPayment.getPaymentDate()).isNull();
    }

    @Test
    void failedPayment_reFailed() {
        //given
        paymentService.failedPayment(paymentId);

        //when
        Payment failedPayment = paymentService.failedPayment(paymentId);

        //then
        assertThat(failedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failedPayment.getPaymentDate()).isNull();
    }

    @Test
    void completePayment() {
        //when
        Payment completePayment = paymentService.completePayment(paymentId);

        //then
        assertThat(completePayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(completePayment.getOrder().getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(completePayment.getPaymentDate()).isNotNull();
    }

    @Test
    void completePayment_fromFailed() {
        //given
        paymentService.failedPayment(paymentId);

        //when
        Payment completePayment = paymentService.completePayment(paymentId);

        //then
        assertThat(completePayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(completePayment.getOrder().getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(completePayment.getPaymentDate()).isNotNull();
    }

    @Test
    void failed_completePayment_alreadyCompletePayment() {
        //given
        paymentService.completePayment(paymentId);

        //when
        assertThrows(IllegalStateException.class, () ->
                paymentService.completePayment(paymentId));
    }

    @Test
    void delete_order() {
        //given
        orderService.cancelOrder(orderNumber1);

        //when
        orderService.deleteOrder(orderNumber1);

        //then
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.findPaymentByPaymentId(paymentId));
    }

    private String createMember() {
        MemberRequest request = new MemberRequest("userA", "id_A", "00000000", "Seoul", "Gangnam", "01234");
        return memberService.join(request);
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

    static Stream<Arguments> orderNumberProvider() {
        return Stream.of(
                arguments(null, "주문 번호 null"),
                arguments("", "주문 번호 빈 문자열"),
                arguments(" ", "주문 번호 공백"),
                arguments("a".repeat(11), "주문 번호 12자 미만"),
                arguments("l".repeat(12), "존재하지 않는 주문 번호"),
                arguments("a".repeat(13), "주문 번호 12자 초과")
        );
    }

    static Stream<Arguments> paymentIdProvider() {
        return Stream.of(
                arguments(null, "결제 번호 null"),
                arguments("", "결제 번호 빈 문자열"),
                arguments(" ", "결제 번호 공백"),
                arguments("a".repeat(35), "결제 번호 36자 미만"),
                arguments("a".repeat(36), "존재하지 않는 결제 번호"),
                arguments("a".repeat(37), "결제 번호 36자 초과")
        );
    }
}
