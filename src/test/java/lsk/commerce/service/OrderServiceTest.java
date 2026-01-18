package lsk.commerce.service;

import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.domain.product.Product;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    MemberService memberService;
    @Autowired
    ProductService productService;
    @Autowired
    OrderService orderService;

    @Test
    void order() {
        //given
        Long memberId = createMember1();
        Long albumId = createAlbum();
        Long bookId = createBook();
        Long movieId = createMovie();

        //when
        Long orderId = orderService.order(memberId, Map.of(albumId, 3, bookId, 5, movieId, 2));

        //then
        Order findOrder = orderService.findOrder(orderId);
        assertThat(findOrder.getOrderProducts().size()).isEqualTo(3);

        Product album = productService.findProduct(albumId);
        Product book = productService.findProduct(bookId);
        Product movie = productService.findProduct(movieId);
        assertThat(findOrder.getOrderProducts())
                .extracting(OrderProduct::getProduct, OrderProduct::getCount)
                .contains(tuple(album, 3), tuple(book, 5), tuple(movie, 2));

        assertThat(album.getStockQuantity()).isEqualTo(17);
    }

    @Test
    void order_fail() {
        //given
        Long memberId = createMember1();
        Long albumId = createAlbum();

        //when
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            orderService.order(memberId, Map.of(albumId, 21));
        });
    }

    @Test
    void updateOrder() {
        //given
        Long memberId = createMember1();
        Long albumId = createAlbum();
        Long bookId = createBook();
        Long movieId = createMovie();

        Long orderId = orderService.order(memberId, Map.of(albumId, 3, bookId, 5, movieId, 2));
        Order order = orderService.findOrder(orderId);

        //when
        orderService.updateOrder(order, Map.of(albumId, 1, bookId, 5));

        //then
        Order findOrder = orderService.findOrder(orderId);
        assertThat(findOrder.getOrderProducts().size()).isEqualTo(2);

        Product album = productService.findProduct(albumId);
        Product book = productService.findProduct(bookId);
        Product movie = productService.findProduct(movieId);
        assertThat(album.getStockQuantity()).isEqualTo(19);
        assertThat(book.getStockQuantity()).isEqualTo(5);
        assertThat(movie.getStockQuantity()).isEqualTo(15);
    }

    @Test
    void deleteOrder() {
        //given
        Long orderId = createOrder();
        Order order = orderService.findOrder(orderId);

        //when
        orderService.DeleteOrder(order);

        //then
        Order findOrder = orderService.findOrder(orderId);
        assertThat(findOrder).isNull();
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
}