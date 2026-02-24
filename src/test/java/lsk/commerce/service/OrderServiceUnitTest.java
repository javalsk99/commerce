package lsk.commerce.service;

import jakarta.persistence.EntityManager;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.dto.request.OrderRequest;
import lsk.commerce.dto.response.OrderResponse;
import lsk.commerce.repository.OrderProductJdbcRepository;
import lsk.commerce.repository.OrderRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    EntityManager em;

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderProductJdbcRepository orderProductJdbcRepository;

    @Mock
    MemberService memberService;

    @Mock
    ProductService productService;

    @InjectMocks
    OrderService orderService;

    @Captor
    ArgumentCaptor<Order> orderCaptor;

    @Captor
    ArgumentCaptor<List<OrderProduct>> orderProductCaptor;

    Member member;
    Delivery delivery;
    Album album;
    Book book;
    Movie movie;

    @BeforeEach
    void beforeEach() {
        member = Member.builder().loginId("id_A").build();
        delivery = new Delivery(member);

        album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build();
        book = Book.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(7).author("김영한").isbn("9788960777330").build();
        movie = Movie.builder().name("범죄도시").price(15000).stockQuantity(5).actor("마동석").director("강윤성").build();
    }

    @Test
    void order() {
        //given
        Map<String, Integer> productMap = Map.of("BANG BANG", 3, "자바 ORM 표준 JPA 프로그래밍", 2, "범죄도시", 4);

        given(memberService.findMemberByLoginId(anyString())).willReturn(member);
        given(productService.findProducts()).willReturn(List.of(album, book, movie));

        InOrder inOrder = inOrder(em, orderRepository, orderProductJdbcRepository);

        //when
        orderService.order("id_A", productMap);

        //then
        assertAll(
                () -> then(memberService).should().findMemberByLoginId(anyString()),
                () -> then(productService).should().findProducts()
        );

        assertAll(
                () -> then(orderRepository).should(inOrder).save(orderCaptor.capture()),
                () -> then(em).should(inOrder).flush(),
                () -> then(orderProductJdbcRepository).should(inOrder).saveAll(orderProductCaptor.capture()),
                () -> then(em).should(inOrder).clear()
        );

        Order order = orderCaptor.getValue();
        List<OrderProduct> orderProducts = orderProductCaptor.getValue();

        assertAll(
                () -> assertThat(order.getOrderProducts()).isEqualTo(orderProducts),
                () -> assertThat(order.getOrderProducts())
                        .extracting("product.name", "product.price", "count", "orderPrice")
                        .containsExactlyInAnyOrder(
                                tuple("BANG BANG", 15000, 3, 45000),
                                tuple("자바 ORM 표준 JPA 프로그래밍", 15000, 2, 30000),
                                tuple("범죄도시", 15000, 4, 60000))
        );
    }

    @Test
    void failed_order_productMapIsEmpty() {
        //given
        Map<String, Integer> emptyProductMap = new HashMap<>();

        //when
        assertThatThrownBy(() -> orderService.order("id_A", emptyProductMap))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문 상품이 없습니다.");

        //then
        assertAll(
                () -> then(memberService).should(never()).findMemberByLoginId(any()),
                () -> then(productService).should(never()).findProducts(),
                () -> then(orderRepository).should(never()).save(any()),
                () -> then(orderProductJdbcRepository).should(never()).saveAll(any())
        );
    }

    @Test
    void failed_order_notExistsMember() {
        //given
        Map<String, Integer> productMap = Map.of("BANG BANG", 3, "자바 ORM 표준 JPA 프로그래밍", 2, "범죄도시", 4);

        given(memberService.findMemberByLoginId(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 아이디입니다."));

        //when
        assertThatThrownBy(() -> orderService.order("id_B", productMap))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 아이디입니다.");

        //then
        assertAll(
                () -> then(memberService).should().findMemberByLoginId(anyString()),
                () -> then(productService).should(never()).findProducts(),
                () -> then(orderRepository).should(never()).save(any()),
                () -> then(orderProductJdbcRepository).should(never()).saveAll(any())
        );
    }

    @ParameterizedTest
    @MethodSource("keyValueProvider")
    void failed_order_productMapHasNull(String key, Integer value, String message) {
        //given
        given(memberService.findMemberByLoginId(anyString())).willReturn(member);
        given(productService.findProducts()).willReturn(List.of(album, book, movie));

        Map<String, Integer> hasNullProductMap = new HashMap<>();
        hasNullProductMap.put(key, value);

        //when
        assertThatThrownBy(() -> orderService.order("id_A", hasNullProductMap))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);

        //then
        assertAll(
                () -> then(memberService).should().findMemberByLoginId(anyString()),
                () -> then(productService).should().findProducts(),
                () -> then(orderRepository).should(never()).save(any()),
                () -> then(orderProductJdbcRepository).should(never()).saveAll(any())
        );
    }

    @Test
    void failed_order_notExistsProduct() {
        //given
        given(memberService.findMemberByLoginId(anyString())).willReturn(member);
        given(productService.findProducts()).willReturn(List.of(album, book, movie));

        Map<String, Integer> notExistsNameProductMap = Map.of("타임 캡슐", 3);

        //when
        assertThatThrownBy(() -> orderService.order("id_A", notExistsNameProductMap))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 상품입니다. name: " + "타임 캡슐");

        //then
        assertAll(
                () -> then(memberService).should().findMemberByLoginId(anyString()),
                () -> then(productService).should().findProducts(),
                () -> then(orderRepository).should(never()).save(any()),
                () -> then(orderProductJdbcRepository).should(never()).saveAll(any())
        );
    }

    @Test
    void find() {
        //given
        Order order = createOrder();

        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));

        //when
        Order findOrder = orderService.findOrder(order.getOrderNumber());

        //then
        assertAll(
                () -> then(orderRepository).should().findByOrderNumber(anyString()),
                () -> assertThat(findOrder).isEqualTo(order)
        );
    }

    @Test
    void failed_find_notExistsOrder() {
        //given
        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.empty());

        //when
        assertThatThrownBy(() -> orderService.findOrder("doshfneijkd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 주문입니다.");

        //then
        then(orderRepository).should().findByOrderNumber(anyString());
    }

    @Test
    void update() {
        //given
        Order order = createOrder();

        given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
        given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
        given(productService.findProducts()).willReturn(List.of(album, book, movie));

        InOrder inOrder = inOrder(em, orderRepository, orderProductJdbcRepository);

        //when
        orderService.updateOrder(order.getOrderNumber(), Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5));

        //then
        assertAll(
                () -> then(orderRepository).should(inOrder).findWithAllExceptMember(anyString()),
                () -> then(em).should(inOrder).flush(),
                () -> then(orderProductJdbcRepository).should(inOrder).deleteOrderProductsByOrderId(order.getId()),
                () -> then(em).should(inOrder).clear(),
                () -> then(orderRepository).should(inOrder).findByOrderNumber(anyString())
        );

        then(productService).should().findProducts();

        assertAll(
                () -> then(em).should(inOrder).flush(),
                () -> then(orderProductJdbcRepository).should(inOrder).saveAll(orderProductCaptor.capture()),
                () -> then(em).should(inOrder).clear()
        );

        List<OrderProduct> orderProducts = orderProductCaptor.getValue();
        assertThat(orderProducts)
                .extracting("product.name", "count")
                .containsExactlyInAnyOrder(tuple("BANG BANG", 2), tuple("자바 ORM 표준 JPA 프로그래밍", 5));
    }

    static Stream<Arguments> keyValueProvider() {
        return Stream.of(
                argumentSet("키 null", null, 3, "상품이 존재하지 않습니다."),
                argumentSet("값 null", "BANG BANG", null, "수량이 존재하지 않습니다."),
                argumentSet("키, 값 모두 null", null, null, "상품이 존재하지 않습니다.")
        );
    }

    private Order createOrder() {
        Delivery delivery = new Delivery(member);

        OrderProduct orderProduct1 = OrderProduct.createOrderProduct(album, 3);
        OrderProduct orderProduct2 = OrderProduct.createOrderProduct(book, 2);
        OrderProduct orderProduct3 = OrderProduct.createOrderProduct(movie, 4);

        return Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2, orderProduct3));
    }

/*
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
        paymentService.completePayment(order.getPayment().getPaymentId());

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

    @Test
    void change_dto() {
        //given
        Order order = orderService.findOrderWithAll(orderNumber);

        //when
        OrderRequest orderRequest = orderService.getOrderRequest(order);
        OrderResponse orderResponse = orderService.getOrderResponse(order);

        //then
        assertThat(orderRequest)
                .extracting("orderNumber", "memberLoginId")
                .containsExactlyInAnyOrder(orderNumber, memberLoginId);
        assertThat(orderResponse)
                .extracting("paymentDate", "shippedDate", "deliveredDate")
                .containsOnlyNulls();
    }
*/
}