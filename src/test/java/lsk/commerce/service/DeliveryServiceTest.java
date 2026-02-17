package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.MemberRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class DeliveryServiceTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

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
    Order order;
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

        orderNumber = orderService.order(memberLoginId, Map.of(album.getName(), 3, book.getName(), 2, movie.getName(), 5));

        order = paymentService.request(orderNumber);
        paymentId = order.getPayment().getPaymentId();
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM order_product");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM member");
        jdbcTemplate.update("DELETE FROM category_product");
        jdbcTemplate.update("DELETE FROM category");
        jdbcTemplate.update("DELETE FROM product");
        jdbcTemplate.update("DELETE FROM payment");
    }

    @Test
    void startDelivery() {
        //given
        paymentService.completePayment(paymentId);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        //when
        deliveryService.startDelivery(orderNumber);

        //then
        TestTransaction.start();
        Order findOrder = orderService.findOrderWithDelivery(orderNumber);
        assertThat(findOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(findOrder.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED);

        TestTransaction.end();
    }

    @Test
    void failed_startDelivery_NotPaidOrder() {
        //given
        TestTransaction.flagForCommit();
        TestTransaction.end();

        //when
        assertThatThrownBy(() -> deliveryService.startDelivery(orderNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("결제가 완료된 주문이 아닙니다.");
    }

    @Test
    void failed_startDelivery_canceledOrder() {
        //given
        orderService.cancelOrder(orderNumber);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        //when
        assertThatThrownBy(() -> deliveryService.startDelivery(orderNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("결제가 완료된 주문이 아닙니다.");
    }

    @Test
    void failed_startDelivery_alreadyShipped() {
        //given
        paymentService.completePayment(paymentId);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        deliveryService.startDelivery(orderNumber);

        //when
        assertThatThrownBy(() -> deliveryService.startDelivery(orderNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 발송된 주문입니다.");
    }

    @Test
    void failed_startDelivery_alreadyDelivered() {
        //given
        paymentService.completePayment(paymentId);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        deliveryService.startDelivery(orderNumber);
        deliveryService.completeDelivery(orderNumber);

        //when
        assertThatThrownBy(() -> deliveryService.startDelivery(orderNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 배송 완료된 주문입니다.");
    }

    @Test
    void completeDelivery() {
        paymentService.completePayment(paymentId);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        deliveryService.startDelivery(orderNumber);

        //when
        deliveryService.completeDelivery(orderNumber);

        //then
        TestTransaction.start();
        Order findOrder = orderService.findOrderWithDelivery(orderNumber);
        assertThat(findOrder.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(findOrder.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);

        TestTransaction.end();
    }

    @Test
    void failed_completeDelivery_NotPaidOrder() {
        //given
        TestTransaction.flagForCommit();
        TestTransaction.end();

        //when
        assertThatThrownBy(() -> deliveryService.completeDelivery(orderNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("결제가 완료된 주문이 아닙니다.");
    }

    @Test
    void failed_completeDelivery_canceledOrder() {
        //given
        orderService.cancelOrder(orderNumber);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        //when
        assertThatThrownBy(() -> deliveryService.completeDelivery(orderNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("결제가 완료된 주문이 아닙니다.");
    }

    @Test
    void failed_completeDelivery_notShipped() {
        //given
        paymentService.completePayment(paymentId);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        //when
        assertThatThrownBy(() -> deliveryService.completeDelivery(orderNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("발송된 주문이 아닙니다.");
    }

    @Test
    void failed_completeDelivery_alreadyDelivered() {
        //given
        paymentService.completePayment(paymentId);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        deliveryService.startDelivery(orderNumber);
        deliveryService.completeDelivery(orderNumber);

        //when
        assertThatThrownBy(() -> deliveryService.completeDelivery(orderNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 배송 완료된 주문입니다.");
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
}