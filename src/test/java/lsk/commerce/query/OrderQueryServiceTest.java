package lsk.commerce.query;

import lsk.commerce.config.QuerydslConfig;
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
import lsk.commerce.dto.response.OrderSearchResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.exception.NotResourceOwnerException;
import lsk.commerce.query.dto.OrderQueryDto;
import lsk.commerce.query.dto.OrderSearchCond;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        OrderQueryRepository.class,
        OrderProductQueryRepository.class,
        OrderQueryService.class,
        QuerydslConfig.class
})
class OrderQueryServiceTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    OrderQueryService orderQueryService;

    String orderNumber1;
    String orderNumber2;
    String orderNumber3;
    String orderNumber4;
    String orderNumber5;
    String orderNumber6;
    String orderNumber7;
    String orderNumber8;

    @BeforeEach
    void beforeEach() {
        initCreateOrders();
        em.flush();
        em.clear();
    }

    @Nested
    class Find {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                System.out.println("================= WHEN START =================");

                //when
                OrderQueryDto orderQueryDto = orderQueryService.findOrder(orderNumber1, "id_A");

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(orderQueryDto).isNotNull();
                    softly.then(Hibernate.isInitialized(orderQueryDto.orderProductQueryDtoList())).isTrue();
                    softly.then(orderQueryDto.orderProductQueryDtoList())
                            .hasSize(2)
                            .extracting("name", "price", "quantity", "orderPrice")
                            .containsExactlyInAnyOrder(tuple("BANG BANG", 15000, 3, 45000), tuple("404", 15000, 4, 60000));
                    softly.then(orderQueryDto.totalAmount()).isEqualTo(105000);
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderNotFound() {
                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> orderQueryService.findOrder("ll1lI1IlOO00", "id_A"))
                        .isInstanceOf(DataNotFoundException.class)
                        .hasMessage("존재하지 않는 주문입니다");

                System.out.println("================= WHEN END ===================");
            }

            @Test
            void orderNotOwner() {
                System.out.println("================= WHEN START =================");

                //when & then
                thenThrownBy(() -> orderQueryService.findOrder(orderNumber1, "id_D"))
                        .isInstanceOf(NotResourceOwnerException.class)
                        .hasMessage("주문의 주인이 아닙니다");

                System.out.println("================= WHEN END ===================");
            }
        }
    }

    @Nested
    class Search {

        @Nested
        class SuccessCase {

            @Test
            void shouldFindAll_WhenCondIsEmpty() {
                //given
                OrderSearchCond cond = OrderSearchCond.builder().build();

                System.out.println("================= WHEN START =================");

                //when
                List<OrderSearchResponse> orderSearchResponseList = orderQueryService.searchOrders(cond);

                System.out.println("================= WHEN END ===================");

                //then
                then(orderSearchResponseList)
                        .extracting("orderNumber", "totalAmount", "orderStatus")
                        .containsExactlyInAnyOrder(
                                tuple(orderNumber1, 105000, OrderStatus.CREATED), tuple(orderNumber2, 30000, OrderStatus.CREATED),
                                tuple(orderNumber3, 60000, OrderStatus.CANCELED), tuple(orderNumber4, 180000, OrderStatus.DELIVERED),
                                tuple(orderNumber5, 45000, OrderStatus.DELIVERED), tuple(orderNumber6, 90000, OrderStatus.PAID),
                                tuple(orderNumber7, 30000, OrderStatus.PAID), tuple(orderNumber8, 165000, OrderStatus.PAID)
                        );
            }

            @Test
            void shouldFilterByMultiple_WhenVariousFieldsProvided() {
                //given
                OrderSearchCond cond = OrderSearchCond.builder()
                        .startDate(LocalDate.of(2026, 3, 8))
                        .endDate(LocalDate.of(2026, 3, 10))
                        .paymentStatus(PaymentStatus.COMPLETED)
                        .build();

                System.out.println("================= WHEN START =================");

                //when
                List<OrderSearchResponse> orderSearchResponseList = orderQueryService.searchOrders(cond);

                System.out.println("================= WHEN END ===================");

                //then
                then(orderSearchResponseList)
                        .extracting("orderNumber", "totalAmount", "orderStatus")
                        .containsExactlyInAnyOrder(tuple(orderNumber4, 180000, OrderStatus.DELIVERED), tuple(orderNumber5, 45000, OrderStatus.DELIVERED));
            }

            @Test
            void shouldReturnEmpty_WhenOrderNotFound() {
                //given
                OrderSearchCond cond = OrderSearchCond.builder()
                        .memberLoginId("id_A")
                        .deliveryStatus(DeliveryStatus.DELIVERED)
                        .build();

                System.out.println("================= WHEN START =================");

                //when
                List<OrderSearchResponse> orderSearchResponseList = orderQueryService.searchOrders(cond);

                System.out.println("================= WHEN END ===================");

                //then
                then(orderSearchResponseList).isEmpty();
            }
        }
    }

    private void initCreateOrders() {
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
        Order order2 = createOrder(member2, orderProductList2);
        Order order3 = createOrder(member1, orderProductList3);
        Order order4 = createOrder(member3, orderProductList4);
        Order order5 = createOrder(member3, orderProductList5);
        Order order6 = createOrder(member1, orderProductList6);
        Order order7 = createOrder(member1, orderProductList7);
        Order order8 = createOrder(member1, orderProductList8);

        orderNumber1 = order1.getOrderNumber();
        orderNumber2 = order2.getOrderNumber();
        orderNumber3 = order3.getOrderNumber();
        orderNumber4 = order4.getOrderNumber();
        orderNumber5 = order5.getOrderNumber();
        orderNumber6 = order6.getOrderNumber();
        orderNumber7 = order7.getOrderNumber();
        orderNumber8 = order8.getOrderNumber();

        persistOrderProducts(orderProduct1, orderProduct2, orderProduct3, orderProduct4, orderProduct5, orderProduct6, orderProduct7, orderProduct8, orderProduct9, orderProduct10, orderProduct11, orderProduct12, orderProduct13, orderProduct14, orderProduct15, orderProduct16);

        Payment.requestPayment(order2);
        Payment.requestPayment(order3);
        Payment.requestPayment(order4);
        Payment.requestPayment(order5);
        Payment.requestPayment(order6);
        Payment.requestPayment(order7);
        Payment.requestPayment(order8);

        order3.cancel();

        em.flush();

        ReflectionTestUtils.setField(order1, "orderDate", LocalDateTime.of(2026, 3, 6, 12, 0, 0));
        ReflectionTestUtils.setField(order2, "orderDate", LocalDateTime.of(2026, 3, 7, 12, 0, 0));
        ReflectionTestUtils.setField(order3, "orderDate", LocalDateTime.of(2026, 3, 8, 12, 0, 0));
        ReflectionTestUtils.setField(order4, "orderDate", LocalDateTime.of(2026, 3, 9, 12, 0, 0));
        ReflectionTestUtils.setField(order5, "orderDate", LocalDateTime.of(2026, 3, 10, 12, 0, 0));
        ReflectionTestUtils.setField(order6, "orderDate", LocalDateTime.of(2026, 3, 11, 12, 0, 0));
        ReflectionTestUtils.setField(order7, "orderDate", LocalDateTime.of(2026, 3, 12, 12, 0, 0));
        ReflectionTestUtils.setField(order8, "orderDate", LocalDateTime.of(2026, 3, 13, 12, 0, 0));
        order4.getPayment().complete(LocalDateTime.now());
        order5.getPayment().complete(LocalDateTime.now());
        order6.getPayment().complete(LocalDateTime.now());
        order7.getPayment().complete(LocalDateTime.now());
        order8.getPayment().complete(LocalDateTime.now());
        order4.completePaid();
        order5.completePaid();
        order6.completePaid();
        order7.completePaid();
        order8.completePaid();
        order4.getDelivery().startDelivery();
        order5.getDelivery().startDelivery();
        order6.getDelivery().startDelivery();
        order7.getDelivery().startDelivery();
        order4.getDelivery().completeDelivery();
        order5.getDelivery().completeDelivery();
    }

    private Member createMember(String loginId) {
        Member member = Member.builder()
                .name("User")
                .loginId(loginId)
                .password("abAB12!@")
                .zipcode("01234")
                .baseAddress("서울시 강남구")
                .detailAddress("101동 101호")
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