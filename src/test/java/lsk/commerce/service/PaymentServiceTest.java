package lsk.commerce.service;

import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static lsk.commerce.domain.PaymentStatus.*;
import static org.assertj.core.api.Assertions.*;

/*
@SpringBootTest
@Transactional
class PaymentServiceTest {

    @Autowired
    MemberService memberService;
    @Autowired
    ProductService productService;
    @Autowired
    OrderService orderService;
    @Autowired
    PaymentService paymentService;

    @Test
    void request() {
        //given
        Long orderNumber = createOrder();
        Order findOrder = orderService.findOrder(orderNumber);

        //when
        Long paymentId = paymentService.request(findOrder);
        Payment findPayment = paymentService.findPayment(paymentId);

        //then
        assertThat(findPayment.getPaymentStatus()).isEqualTo(PENDING);
        assertThat(findPayment.getPaymentAmount()).isEqualTo(findOrder.getTotalAmount());
    }

    private Long createMember1() {
        Member member = new Member("userA", "idA", "0000", "Seoul", "Gangnam", "01234");
        memberService.join(member);
        return member.getId();
    }

    private Long createMember2() {
        Member member = new Member("userB", "idB", "1111", "Seoul", "Gangbuk", "01235");
        memberService.join(member);
        return member.getId();
    }

    private Long createAlbum() {
        Album album = new Album("하얀 그리움", 15000, 20, "fromis_9", "ASND");
        productService.register(album);
        return album.getId();
    }

    private Long createBook() {
        Book book = new Book("자바 ORM 표준 JPA 프로그래밍", 43000, 10, "김영한", "9788960777330");
        productService.register(book);
        return book.getId();
    }

    private Long createMovie() {
        Movie movie = new Movie("굿뉴스", 7000, 15, "변성현", "설경구");
        productService.register(movie);
        return movie.getId();
    }

    private Long createOrder() {
        Long memberId = createMember1();
        Long albumId = createAlbum();
        Long bookId = createBook();
        Long movieId = createMovie();
        return orderService.order(memberId, Map.of(albumId, 3, bookId, 5, movieId, 2));
    }
}*/
