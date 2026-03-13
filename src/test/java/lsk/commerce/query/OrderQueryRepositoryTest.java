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
import lsk.commerce.query.dto.OrderQueryDto;
import lsk.commerce.query.dto.OrderSearchCond;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        OrderQueryRepository.class,
        QuerydslConfig.class
})
class OrderQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    OrderQueryRepository orderQueryRepository;

    String orderNumber1;
    String orderNumber2;
    String orderNumber3;

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
                            .containsExactly(30000, OrderStatus.CREATED, PaymentStatus.PENDING, DeliveryStatus.WAITING);
                    softly.then(orderQueryDto.get())
                            .extracting("loginId", "paymentDate", "shippedDate", "deliveredDate")
                            .containsOnlyNulls();
                });
            }

            @Test
            void orderByOrderNumber_OrderStatusIsCanceled() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<OrderQueryDto> orderQueryDto = orderQueryRepository.findOrderByOrderNumber(orderNumber3);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(orderQueryDto).isPresent();
                    softly.then(orderQueryDto.get().getOrderProducts()).isEmpty();
                    softly.then(orderQueryDto.get())
                            .extracting("totalAmount", "orderStatus", "paymentStatus", "deliveryStatus")
                            .containsExactly(60000, OrderStatus.CANCELED, PaymentStatus.CANCELED, DeliveryStatus.CANCELED);
                    softly.then(orderQueryDto.get())
                            .extracting("loginId", "paymentDate", "shippedDate", "deliveredDate")
                            .containsOnlyNulls();
                });
            }

            @Test
            void orderByOrderNumber_ShouldReturnEmpty_WhenOrderNumberNotFound() {
                System.out.println("================= WHEN START =================");

                //when
                Optional<OrderQueryDto> orderQueryDto = orderQueryRepository.findOrderByOrderNumber("ll1lI1IlOO00");

                System.out.println("================= WHEN END ===================");

                //then
                then(orderQueryDto).isEmpty();
            }

            @Test
            void ordersByLoginId() {
                System.out.println("================= WHEN START =================");

                //when
                List<OrderQueryDto> orderQueryDtoList = orderQueryRepository.findOrdersByLoginId("id_A");

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(orderQueryDtoList)
                            .extracting("orderProducts")
                            .containsOnly(Collections.emptyList());
                    softly.then(orderQueryDtoList)
                            .hasSize(5)
                            .extracting("totalAmount")
                            .containsExactlyInAnyOrder(105000, 60000, 90000, 30000, 165000);
                });
            }

            @Test
            void ordersByLoginId_ShouldReturnEmpty_WhenLoginIdNotFound() {
                System.out.println("================= WHEN START =================");

                //when
                List<OrderQueryDto> orderQueryDtoList = orderQueryRepository.findOrdersByLoginId("id_D");

                System.out.println("================= WHEN END ===================");

                //then
                then(orderQueryDtoList).isEmpty();
            }

            @Test
            void ordersByLoginIds() {
                System.out.println("================= WHEN START =================");

                //when
                List<OrderQueryDto> orderQueryDtoList = orderQueryRepository.findOrdersByLoginIds(List.of("id_B", "id_C"));

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(orderQueryDtoList)
                            .extracting("orderProducts")
                            .containsOnly(Collections.emptyList());
                    softly.then(orderQueryDtoList)
                            .hasSize(3)
                            .extracting("loginId", "totalAmount")
                            .containsExactlyInAnyOrder(tuple("id_B", 30000), tuple("id_C", 180000), tuple("id_C", 45000));
                });
            }

            @Test
            void ordersByLoginIds_ShouldReturnExisting_WhenLoginIdsNotFound() {
                System.out.println("================= WHEN START =================");

                //when
                List<OrderQueryDto> orderQueryDtoList = orderQueryRepository.findOrdersByLoginIds(List.of("id_A", "id_D"));

                System.out.println("================= WHEN END ===================");

                //then
                then(orderQueryDtoList)
                            .hasSize(5)
                            .extracting("loginId")
                            .containsOnly("id_A");
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
                List<OrderQueryDto> orderQueryDtoList = orderQueryRepository.search(cond);

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(orderQueryDtoList)
                            .extracting("orderProducts")
                            .containsOnly(Collections.emptyList());
                    softly.then(orderQueryDtoList)
                            .hasSize(8)
                            .extracting("loginId", "totalAmount")
                            .containsExactlyInAnyOrder(tuple("id_A", 105000), tuple("id_B", 30000), tuple("id_A", 60000), tuple("id_C", 180000), tuple("id_C", 45000), tuple("id_A", 90000), tuple("id_A", 30000), tuple("id_A", 165000));
                });
            }

            @ParameterizedTest
            @MethodSource("memberLoginIdCondProvider")
            void shouldFilterByExactMemberLoginId_WhenMemberLoginIdIsPresent(OrderSearchCond cond, int size, List<Integer> amounts) {
                assertThatContainsExactlyAmounts(cond, size, amounts);
            }

            @ParameterizedTest
            @MethodSource("productNameCondProvider")
            void shouldFilterByProductName_WhenProductNameIsPresent(OrderSearchCond cond, int size, List<Integer> amounts) {
                assertThatContainsExactlyAmounts(cond, size, amounts);
            }

            @ParameterizedTest
            @MethodSource("orderStatusCondProvider")
            void shouldFilterByExactOrderStatus_WhenOrderStatusIsPresent(OrderSearchCond cond, int size, List<Integer> amounts) {
                assertThatContainsExactlyAmounts(cond, size, amounts);
            }

            @ParameterizedTest
            @MethodSource("dateRangeCondProvider")
            void shouldFilterByDateRange_WhenDatesAreOptional(OrderSearchCond cond, int size, List<Integer> amounts) {
                assertThatContainsExactlyAmounts(cond, size, amounts);
            }

            @ParameterizedTest
            @MethodSource("paymentStatusCondProvider")
            void shouldFilterByExactPaymentStatus_WhenPaymentStatusIsPresent(OrderSearchCond cond, int size, List<Integer> amounts) {
                assertThatContainsExactlyAmounts(cond, size, amounts);
            }

            @ParameterizedTest
            @MethodSource("deliveryStatusCondProvider")
            void shouldFilterByExactDeliveryStatus_WhenDeliveryStatusIsPresent(OrderSearchCond cond, int size, List<Integer> amounts) {
                assertThatContainsExactlyAmounts(cond, size, amounts);
            }

            @ParameterizedTest
            @MethodSource("variousFieldsCondProvider")
            void shouldFilterByMultiple_WhenVariousFieldsProvided(OrderSearchCond cond, int size, List<Integer> amounts) {
                assertThatContainsExactlyAmounts(cond, size, amounts);
            }

            static Stream<Arguments> memberLoginIdCondProvider() {
                return Stream.of(
                        argumentSet("로그인 아이디를 id로 검색", OrderSearchCond.builder().memberLoginId("id").build(), 0, Collections.emptyList()),
                        argumentSet("로그인 아이디를 id_A로 검색", OrderSearchCond.builder().memberLoginId("id_A").build(), 5, List.of(105000, 60000, 90000, 30000, 165000)),
                        argumentSet("로그인 아이디를 id_B로 검색", OrderSearchCond.builder().memberLoginId("id_B").build(), 1, List.of(30000)),
                        argumentSet("로그인 아이디를 id_C로 검색", OrderSearchCond.builder().memberLoginId("id_C").build(), 2, List.of(180000, 45000)),
                        argumentSet("로그인 아이디를 id_a로 검색", OrderSearchCond.builder().memberLoginId("id_a").build(), 5, List.of(105000, 60000, 90000, 30000, 165000))
                );
            }

            static Stream<Arguments> productNameCondProvider() {
                return Stream.of(
                        argumentSet("상품 이름을 BANG BANG으로 검색", OrderSearchCond.builder().productName("BANG BANG").build(), 2, List.of(105000, 165000)),
                        argumentSet("상품 이름을 b로 검색", OrderSearchCond.builder().productName("b").build(), 4, List.of(105000, 30000, 90000, 165000)),
                        argumentSet("상품 이름을 ㅂ으로 검색", OrderSearchCond.builder().productName("ㅂ").build(), 5, List.of(60000, 45000, 90000, 30000, 165000)),
                        argumentSet("상품 이름을 ㄱㄴㄷ으로 검색", OrderSearchCond.builder().productName("ㄱㄴㄷ").build(), 0, Collections.emptyList())
                );
            }

            static Stream<Arguments> orderStatusCondProvider() {
                return Stream.of(
                        argumentSet("주문 상태를 CREATED로 검색", OrderSearchCond.builder().orderStatus(OrderStatus.CREATED).build(), 2, List.of(105000, 30000)),
                        argumentSet("주문 상태를 PAID로 검색", OrderSearchCond.builder().orderStatus(OrderStatus.PAID).build(), 3, List.of(90000, 30000, 165000)),
                        argumentSet("주문 상태를 DELIVERED로 검색", OrderSearchCond.builder().orderStatus(OrderStatus.DELIVERED).build(), 2, List.of(180000, 45000)),
                        argumentSet("주문 상태를 CANCELED로 검색", OrderSearchCond.builder().orderStatus(OrderStatus.CANCELED).build(), 1, List.of(60000))
                );
            }

            static Stream<Arguments> dateRangeCondProvider() {
                return Stream.of(
                        argumentSet("3월 8일부터의 주문", OrderSearchCond.builder().startDate(LocalDate.of(2026, 3, 8)).build(), 6, List.of(60000, 180000, 45000, 90000, 30000, 165000)),
                        argumentSet("3월 10일까지의 주문", OrderSearchCond.builder().endDate(LocalDate.of(2026, 3, 10)).build(), 5, List.of(105000, 30000, 60000, 180000, 45000)),
                        argumentSet("3월 8일부터, 3월 10일까지의 주문", OrderSearchCond.builder().startDate(LocalDate.of(2026, 3, 8)).endDate(LocalDate.of(2026, 3, 10)).build(), 3, List.of(60000, 180000, 45000)),
                        argumentSet("3월 10일부터, 3월 8일까지의 주문", OrderSearchCond.builder().startDate(LocalDate.of(2026, 3, 10)).endDate(LocalDate.of(2026, 3, 8)).build(), 0, Collections.emptyList())
                );
            }

            static Stream<Arguments> paymentStatusCondProvider() {
                return Stream.of(
                        argumentSet("결제 상태를 PENDING으로 검색", OrderSearchCond.builder().paymentStatus(PaymentStatus.PENDING).build(), 1, List.of(30000)),
                        argumentSet("결제 상태를 COMPLETED로 검색", OrderSearchCond.builder().paymentStatus(PaymentStatus.COMPLETED).build(), 5, List.of(180000, 45000, 90000, 30000, 165000)),
                        argumentSet("결제 상태를 FAILED로 검색", OrderSearchCond.builder().paymentStatus(PaymentStatus.FAILED).build(), 0, Collections.emptyList()),
                        argumentSet("결제 상태를 CANCELED로 검색", OrderSearchCond.builder().paymentStatus(PaymentStatus.CANCELED).build(), 1, List.of(60000))
                );
            }

            static Stream<Arguments> deliveryStatusCondProvider() {
                return Stream.of(
                        argumentSet("배송 상태를 WAITING으로 검색", OrderSearchCond.builder().deliveryStatus(DeliveryStatus.WAITING).build(), 2, List.of(105000, 30000)),
                        argumentSet("배송 상태를 PREPARING으로 검색", OrderSearchCond.builder().deliveryStatus(DeliveryStatus.PREPARING).build(), 1, List.of(165000)),
                        argumentSet("배송 상태를 SHIPPED로 검색", OrderSearchCond.builder().deliveryStatus(DeliveryStatus.SHIPPED).build(), 2, List.of(90000, 30000)),
                        argumentSet("배송 상태를 DELIVERED로 검색", OrderSearchCond.builder().deliveryStatus(DeliveryStatus.DELIVERED).build(), 2, List.of(180000, 45000)),
                        argumentSet("배송 상태를 CANCELED로 검색", OrderSearchCond.builder().deliveryStatus(DeliveryStatus.CANCELED).build(), 1, List.of(60000))
                );
            }

            static Stream<Arguments> variousFieldsCondProvider() {
                return Stream.of(
                        argumentSet("로그인 아이디를 id_A, 배송 상태를 DELIVERED로 검색",
                                OrderSearchCond.builder()
                                        .memberLoginId("id_A")
                                        .deliveryStatus(DeliveryStatus.DELIVERED)
                                        .build(),
                                0, Collections.emptyList()
                        ),
                        argumentSet("3월 8일부터, 3월 10일까지의 주문, 결제 상태를 COMPLETED로 검색",
                                OrderSearchCond.builder()
                                        .startDate(LocalDate.of(2026, 3, 8))
                                        .endDate(LocalDate.of(2026, 3, 10))
                                        .paymentStatus(PaymentStatus.COMPLETED)
                                        .build(),
                                2, List.of(180000, 45000)
                        ),
                        argumentSet("로그인 아이디를 id_A, 상품 이름을 ㅌ, 주문 상태를 CANCELED로 검색",
                                OrderSearchCond.builder()
                                        .memberLoginId("id_A")
                                        .productName("ㅌ")
                                        .orderStatus(OrderStatus.CANCELED)
                                        .build(),
                                1, List.of(60000)
                        )
                );
            }

            private void assertThatContainsExactlyAmounts(OrderSearchCond cond, int size, List<Integer> amounts) {
                System.out.println("================= WHEN START =================");

                //when
                List<OrderQueryDto> orderQueryDtoList = orderQueryRepository.search(cond);

                System.out.println("================= WHEN END ===================");

                //then
                then(orderQueryDtoList)
                        .hasSize(size)
                        .extracting("totalAmount")
                        .containsExactlyInAnyOrderElementsOf(amounts);
            }
        }
    }

    @Nested
    class ExtractOrderNumbers {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                OrderQueryDto orderQueryDto1 = OrderQueryDto.builder()
                        .orderNumber(orderNumber1)
                        .build();
                OrderQueryDto orderQueryDto2 = OrderQueryDto.builder()
                        .orderNumber(orderNumber2)
                        .build();
                OrderQueryDto orderQueryDto3 = OrderQueryDto.builder()
                        .orderNumber(orderNumber3)
                        .build();
                List<OrderQueryDto> orderQueryDtoList = List.of(orderQueryDto1, orderQueryDto2, orderQueryDto3);

                System.out.println("================= WHEN START =================");

                //when
                List<String> orderNumbers = orderQueryRepository.extractOrderNumbers(orderQueryDtoList);

                System.out.println("================= WHEN END ===================");

                //then
                then(orderNumbers)
                        .hasSize(3)
                        .containsExactlyInAnyOrder(orderNumber1, orderNumber2, orderNumber3);
            }

            @Test
            void shouldReturnEmpty_WhenOrderNumberIsNull() {
                //given
                OrderQueryDto orderQueryDto = OrderQueryDto.builder().build();

                System.out.println("================= WHEN START =================");

                //when
                List<String> orderNumbers = orderQueryRepository.extractOrderNumbers(List.of(orderQueryDto));

                System.out.println("================= WHEN END ===================");

                //then
                then(orderNumbers).isEmpty();
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