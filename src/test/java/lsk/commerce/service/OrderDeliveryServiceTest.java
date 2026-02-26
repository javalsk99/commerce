package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.MemberRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class OrderDeliveryServiceTest {

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
        memberLoginId = createMember();

        category1 = createCategory1();
        category2 = createCategory2();
        category3 = createCategory3();

        album = createAlbum();
        book = createBook();
        movie = createMovie();
        productService.register(album, List.of(category1.getName()));
        productService.register(book, List.of(category2.getName()));
        productService.register(movie, List.of(category3.getName()));

        orderNumber = orderService.order(memberLoginId, Map.of(album.getName(), 3, book.getName(), 2, movie.getName(), 5));
    }

    @Test
    void delete_deliveredOrder() {
        //given
        Order order = paymentService.request(orderNumber);
//        paymentService.completePayment(order.getPayment().getPaymentId());

        TestTransaction.flagForCommit();
        TestTransaction.end();

        deliveryService.startDelivery(orderNumber);
        deliveryService.completeDelivery(orderNumber);

        //when
        orderService.deleteOrder(orderNumber);

        //then
        assertThrows(IllegalArgumentException.class, () ->
                orderService.findOrderWithDeliveryPayment(orderNumber));
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
