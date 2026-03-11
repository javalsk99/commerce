package lsk.commerce.query;

import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.query.dto.OrderQueryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.assertj.core.api.BDDAssertions.*;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderQueryRepository.class)
class OrderQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    OrderQueryRepository orderQueryRepository;

    String orderNumber1;
    String orderNumber2;

    @BeforeEach
    void beforeEach() {
        initCreateProductsAndOrders();
        em.flush();
        em.clear();
    }

    @Nested
    class Find {

        @Nested
        class SuccessCase {

            @Test
            void orderByOrderNumber_HasNoPayment() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<OrderQueryDto> orderQueryDto = orderQueryRepository.findOrderByOrderNumber(orderNumber1);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(orderQueryDto).isPresent();
                    softly.then(orderQueryDto.get().getOrderProducts()).isEmpty();
                    softly.then(orderQueryDto.get())
                            .extracting("totalAmount", "orderStatus", "deliveryStatus")
                            .containsExactly(105000, OrderStatus.CREATED, DeliveryStatus.WAITING);
                    softly.then(orderQueryDto.get())
                            .extracting("loginId", "paymentStatus", "paymentDate", "shippedDate", "deliveredDate")
                            .containsOnlyNulls();
                });
            }

            @Test
            void orderByOrderNumber_HasPayment() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<OrderQueryDto> orderQueryDto = orderQueryRepository.findOrderByOrderNumber(orderNumber2);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(orderQueryDto).isPresent();
                    softly.then(orderQueryDto.get().getOrderProducts()).isEmpty();
                    softly.then(orderQueryDto.get())
                            .extracting("totalAmount", "orderStatus", "paymentStatus", "deliveryStatus")
                            .containsExactly(60000, OrderStatus.CREATED, PaymentStatus.PENDING, DeliveryStatus.WAITING);
                    softly.then(orderQueryDto.get())
                            .extracting("loginId", "paymentDate", "shippedDate", "deliveredDate")
                            .containsOnlyNulls();
                });
            }

            @Test
            void ordersByLoginId() {
                System.out.println("================= WHEN START =================");

                //when
                List<OrderQueryDto> orderQueryDtoList = orderQueryRepository.findOrdersByLoginId("id_A");

                System.out.println("================= WHEN END ===================");

                //then
                then(orderQueryDtoList)
                        .hasSize(5)
                        .extracting("totalAmount")
                        .containsExactlyInAnyOrder(105000, 60000, 90000, 30000, 165000);
            }

            @Test
            void ordersByLoginIds() {
                System.out.println("================= WHEN START =================");

                //when
                List<OrderQueryDto> orderQueryDtoList = orderQueryRepository.findOrdersByLoginIds(List.of("id_B", "id_C"));

                System.out.println("================= WHEN END ===================");

                //then
                then(orderQueryDtoList)
                        .hasSize(3)
                        .extracting("loginId", "totalAmount")
                        .containsExactlyInAnyOrder(tuple("id_B", 30000), tuple("id_C", 180000), tuple("id_C", 45000));
            }
        }
    }

    @Nested
    class Search {

        @Nested
        class SuccessCase {

            @Test
            void basic() {

            }
        }
    }

    private void initCreateProductsAndOrders() {
        Member member1 = createMember("id_A");
        Member member2 = createMember("id_B");
        Member member3 = createMember("id_C");

        Album album1 = createAlbum("BANG BANG", "IVE", "STARSHIP");
        Album album2 = createAlbum("Blue Valentine", "NMIXX", "JYP");
        Album album3 = createAlbum("404", "KiiiKiii", "STARSHIP");
        Album album4 = createAlbum("타임 캡슐", "다비치", "씨에이엠위더스");
        Album album5 = createAlbum("너의 모든 순간", "성시경", "에스케이재원");
        Album album6 = createAlbum("천상연", "이창섭", "판타지오");
        Book book1 = createBook("자바 ORM 표준 JPA 프로그래밍", "김영한", "9788960777330");
        Book book2 = createBook("면접을 위한 CS 전공지식 노트", "주홍철", "9791165219529");
        Book book3 = createBook("Do it! 점프 투 파이썬", "박응용", "9791163034735");
        Movie movie1 = createMovie("범죄도시", "마동석", "강윤성");
        Movie movie2 = createMovie("범죄도시2", "마동석", "이상용");
        Movie movie3 = createMovie("범죄도시3", "마동석", "이상용");
        Movie movie4 = createMovie("범죄도시4", "마동석", "허명행");

        OrderProduct orderProduct1 = OrderProduct.createOrderProduct(album1, 3);
        OrderProduct orderProduct2 = OrderProduct.createOrderProduct(album2, 2);
        OrderProduct orderProduct3 = OrderProduct.createOrderProduct(album3, 4);
        OrderProduct orderProduct4 = OrderProduct.createOrderProduct(album4, 1);
        OrderProduct orderProduct5 = OrderProduct.createOrderProduct(album5, 4);
        OrderProduct orderProduct6 = OrderProduct.createOrderProduct(album6, 3);
        OrderProduct orderProduct7 = OrderProduct.createOrderProduct(book1, 3);
        OrderProduct orderProduct8 = OrderProduct.createOrderProduct(book2, 5);
        OrderProduct orderProduct9 = OrderProduct.createOrderProduct(book3, 2);
        OrderProduct orderProduct10 = OrderProduct.createOrderProduct(movie1, 1);
        OrderProduct orderProduct11 = OrderProduct.createOrderProduct(movie2, 2);
        OrderProduct orderProduct12 = OrderProduct.createOrderProduct(movie3, 5);
        OrderProduct orderProduct13 = OrderProduct.createOrderProduct(movie4, 3);
        OrderProduct orderProduct14 = OrderProduct.createOrderProduct(album1, 4);
        OrderProduct orderProduct15 = OrderProduct.createOrderProduct(album2, 3);
        OrderProduct orderProduct16 = OrderProduct.createOrderProduct(book1, 2);

        List<OrderProduct> orderProductList1 = List.of(orderProduct1, orderProduct3);
        List<OrderProduct> orderProductList2 = List.of(orderProduct2);
        List<OrderProduct> orderProductList3 = List.of(orderProduct4, orderProduct7);
        List<OrderProduct> orderProductList4 = List.of(orderProduct5, orderProduct6, orderProduct8);
        List<OrderProduct> orderProductList5 = List.of(orderProduct9, orderProduct10);
        List<OrderProduct> orderProductList6 = List.of(orderProduct13, orderProduct15);
        List<OrderProduct> orderProductList7 = List.of(orderProduct16);
        List<OrderProduct> orderProductList8 = List.of(orderProduct11, orderProduct12, orderProduct14);

        Order order1 = createOrder(member1, orderProductList1);
        createOrder(member2, orderProductList2);
        Order order3 = createOrder(member1, orderProductList3);
        createOrder(member3, orderProductList4);
        Order order5 = createOrder(member3, orderProductList5);
        createOrder(member1, orderProductList6);
        createOrder(member1, orderProductList7);
        Order order8 = createOrder(member1, orderProductList8);

        orderNumber1 = order1.getOrderNumber();
        orderNumber2 = order3.getOrderNumber();

        persistOrderProducts(orderProduct1, orderProduct2, orderProduct3, orderProduct4, orderProduct5, orderProduct6, orderProduct7, orderProduct8, orderProduct9, orderProduct10, orderProduct11, orderProduct12, orderProduct13, orderProduct14, orderProduct15, orderProduct16);

        Payment.requestPayment(order3);
        Payment.requestPayment(order5);
        Payment.requestPayment(order8);
    }

    private Member createMember(String loginId) {
        Member member = Member.builder()
                .name("User")
                .loginId(loginId)
                .password("00000000")
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
                .build();
        em.persist(member);
        return member;
    }

    private Album createAlbum(String name, String artist, String studio) {
        Album album = Album.builder()
                .name(name)
                .price(15000)
                .stockQuantity(10)
                .artist(artist)
                .studio(studio)
                .build();
        em.persist(album);
        return album;
    }

    private Book createBook(String name, String author, String isbn) {
        Book book = Book.builder()
                .name(name)
                .price(15000)
                .stockQuantity(7)
                .author(author)
                .isbn(isbn)
                .build();
        em.persist(book);
        return book;
    }

    private Movie createMovie(String name, String actor, String director) {
        Movie movie = Movie.builder()
                .name(name)
                .price(15000)
                .stockQuantity(5)
                .actor(actor)
                .director(director)
                .build();
        em.persist(movie);
        return movie;
    }

    private Order createOrder(Member member, List<OrderProduct> orderProductList1) {
        Delivery delivery = new Delivery(member);
        Order order = Order.createOrder(member, delivery, orderProductList1);
        em.persist(order);
        return order;
    }

    private void persistOrderProducts(OrderProduct... orderProducts) {
        for (OrderProduct orderProduct : orderProducts) {
            em.persist(orderProduct);
        }
    }
}