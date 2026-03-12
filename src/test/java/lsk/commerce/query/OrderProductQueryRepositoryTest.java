package lsk.commerce.query;

import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.query.dto.OrderProductQueryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderProductQueryRepository.class)
class OrderProductQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    OrderProductQueryRepository orderProductQueryRepository;

    String orderNumber1;
    String orderNumber2;
    String orderNumber3;
    String orderNumber4;
    String wrongOrderNumber = "lIIlllllIIIl";

    @BeforeEach
    void beforeEach() {
        initCreateOrderProducts();
        em.flush();
        em.clear();
    }

    @Nested
    class Find {

        @Nested
        class SuccessCase {

            @Test
            void orderProductListByOrderNumber() {
                System.out.println("================= WHEN START =================");

                //when
                List<OrderProductQueryDto> orderProductQueryDtoList = orderProductQueryRepository.findOrderProductListByOrderNumber(orderNumber1);

                System.out.println("================= WHEN END ===================");

                //then
                then(orderProductQueryDtoList)
                            .hasSize(2)
                            .extracting("name", "price", "count", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 15000, 3, 45000),
                                    tuple("404", 15000, 4, 60000)
                            );
            }

            @Test
            void shouldReturnEmpty_WhenOrderNumberNotFound() {
                System.out.println("================= WHEN START =================");

                //when
                List<OrderProductQueryDto> orderProductQueryDtoList = orderProductQueryRepository.findOrderProductListByOrderNumber(wrongOrderNumber);

                System.out.println("================= WHEN END ===================");

                //then
                then(orderProductQueryDtoList).isEmpty();
            }

            @Test
            void orderProductListByOrderNumbers() {
                System.out.println("================= WHEN START =================");

                //when
                Map<String, List<OrderProductQueryDto>> orderProductQueryDtoMap = orderProductQueryRepository.findOrderProductListByOrderNumbers(List.of(orderNumber2, orderNumber3));

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(orderProductQueryDtoMap)
                            .hasSize(2)
                            .extractingByKey(orderNumber2)
                            .asInstanceOf(list(OrderProductQueryDto.class))
                            .hasSize(1)
                            .extracting("name", "price", "count", "orderPrice")
                            .containsExactly(tuple("Blue Valentine", 15000, 2, 30000));
                    softly.then(orderProductQueryDtoMap)
                            .hasSize(2)
                            .extractingByKey(orderNumber3)
                            .asInstanceOf(list(OrderProductQueryDto.class))
                            .hasSize(2)
                            .extracting("name", "price", "count", "orderPrice")
                            .containsExactly(
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 15000, 1, 15000),
                                    tuple("범죄도시2", 15000, 3, 45000)
                            );
                });
            }

            @Test
            void orderProductListByOrderNumbers_ShouldReturnExisting_WhenOrderNumbersNotFound() {
                System.out.println("================= WHEN START =================");

                //when
                Map<String, List<OrderProductQueryDto>> orderProductQueryDtoMap = orderProductQueryRepository.findOrderProductListByOrderNumbers(List.of(orderNumber4, wrongOrderNumber));

                System.out.println("================= WHEN END ===================");

                //then
                thenSoftly(softly -> {
                    softly.then(orderProductQueryDtoMap)
                            .hasSize(1)
                            .extractingByKey(orderNumber4)
                            .asInstanceOf(list(OrderProductQueryDto.class))
                            .hasSize(3)
                            .extracting("name", "price", "count", "orderPrice")
                            .containsExactly(
                                    tuple("면접을 위한 CS 전공지식 노트", 15000, 4, 60000),
                                    tuple("범죄도시", 15000, 3, 45000),
                                    tuple("범죄도시3", 15000, 5, 75000)
                            );
                    softly.then(orderProductQueryDtoMap).doesNotContainKey(wrongOrderNumber);
                });
            }
        }
    }

    private void initCreateOrderProducts() {
        Member member1 = createMember("id_A");
        Member member2 = createMember("id_B");
        Member member3 = createMember("id_C");

        Album album1 = createAlbum("BANG BANG", "IVE", "STARSHIP");
        Album album2 = createAlbum("Blue Valentine", "NMIXX", "JYP");
        Album album3 = createAlbum("404", "KiiiKiii", "STARSHIP");
        Book book1 = createBook("자바 ORM 표준 JPA 프로그래밍", "김영한", "9788960777330");
        Book book2 = createBook("면접을 위한 CS 전공지식 노트", "주홍철", "9791165219529");
        Movie movie1 = createMovie("범죄도시", "마동석", "강윤성");
        Movie movie2 = createMovie("범죄도시2", "마동석", "이상용");
        Movie movie3 = createMovie("범죄도시3", "마동석", "이상용");

        OrderProduct orderProduct1 = OrderProduct.createOrderProduct(album1, 3);
        OrderProduct orderProduct2 = OrderProduct.createOrderProduct(album2, 2);
        OrderProduct orderProduct3 = OrderProduct.createOrderProduct(album3, 4);
        OrderProduct orderProduct4 = OrderProduct.createOrderProduct(book1, 1);
        OrderProduct orderProduct5 = OrderProduct.createOrderProduct(book2, 4);
        OrderProduct orderProduct6 = OrderProduct.createOrderProduct(movie1, 3);
        OrderProduct orderProduct7 = OrderProduct.createOrderProduct(movie2, 3);
        OrderProduct orderProduct8 = OrderProduct.createOrderProduct(movie3, 5);

        List<OrderProduct> orderProductList1 = List.of(orderProduct1, orderProduct3);
        List<OrderProduct> orderProductList2 = List.of(orderProduct2);
        List<OrderProduct> orderProductList3 = List.of(orderProduct4, orderProduct7);
        List<OrderProduct> orderProductList4 = List.of(orderProduct5, orderProduct6, orderProduct8);

        orderNumber1 = createOrder(member1, orderProductList1);
        orderNumber2 = createOrder(member2, orderProductList2);
        orderNumber3 = createOrder(member1, orderProductList3);
        orderNumber4 = createOrder(member3, orderProductList4);

        persistOrderProducts(orderProduct1, orderProduct2, orderProduct3, orderProduct4, orderProduct5, orderProduct6, orderProduct7, orderProduct8);
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

    private String createOrder(Member member, List<OrderProduct> orderProductList1) {
        Delivery delivery = new Delivery(member);
        Order order = Order.createOrder(member, delivery, orderProductList1);
        em.persist(order);
        return order.getOrderNumber();
    }

    private void persistOrderProducts(OrderProduct... orderProducts) {
        for (OrderProduct orderProduct : orderProducts) {
            em.persist(orderProduct);
        }
    }
}