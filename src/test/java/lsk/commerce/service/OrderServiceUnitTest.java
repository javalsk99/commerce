package lsk.commerce.service;

import jakarta.persistence.EntityManager;
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
import lsk.commerce.dto.request.OrderRequest;
import lsk.commerce.dto.response.OrderResponse;
import lsk.commerce.repository.OrderProductJdbcRepository;
import lsk.commerce.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.BDDMockito.*;

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
    String wrongOrderNumber = "lllIIllIO00O";

    @BeforeEach
    void beforeEach() {
        member = Member.builder().loginId("id_A").build();
        delivery = new Delivery(member);

        album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).artist("IVE").studio("STARSHIP").build();
        book = Book.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(7).author("김영한").isbn("9788960777330").build();
        movie = Movie.builder().name("범죄도시").price(15000).stockQuantity(5).actor("마동석").director("강윤성").build();
    }

    @Nested
    class SuccessCase {

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
                                    tuple("범죄도시", 15000, 4, 60000)),
                    () -> assertThat(order.getTotalAmount()).isEqualTo(135000)
            );
            assertAll(
                    () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED),
                    () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.WAITING),
                    () -> assertThat(order.getPayment()).isNull()
            );
            assertAll(
                    () -> assertThat(album.getStockQuantity()).isEqualTo(7),
                    () -> assertThat(book.getStockQuantity()).isEqualTo(5),
                    () -> assertThat(movie.getStockQuantity()).isEqualTo(1)
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
            assertAll(
                    () -> assertThat(orderProducts)
                            .extracting("product.name", "count", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 2, 30000),
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 5, 75000)),
                    () -> assertThat(orderProducts.getFirst().getOrder().getTotalAmount()).isEqualTo(105000)
            );
            assertAll(
                    () -> assertThat(album.getStockQuantity()).isEqualTo(8),
                    () -> assertThat(book.getStockQuantity()).isEqualTo(2),
                    () -> assertThat(movie.getStockQuantity()).isEqualTo(5)
            );
        }

        @Test
        void update_idempotency() {
            //given
            Order order = createOrder();

            given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
            given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
            given(productService.findProducts()).willReturn(List.of(album, book, movie));

            //when 첫 번째 호출
            orderService.updateOrder(order.getOrderNumber(), Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5));

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithAllExceptMember(anyString()),
                    () -> then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(order.getId()),
                    () -> then(orderRepository).should().findByOrderNumber(anyString()),
                    () -> then(productService).should().findProducts(),
                    () -> then(orderProductJdbcRepository).should().saveAll(orderProductCaptor.capture())
            );

            List<OrderProduct> orderProducts = orderProductCaptor.getValue();
            assertAll(
                    () -> assertThat(orderProducts)
                            .extracting("product.name", "count", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 2, 30000),
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 5, 75000)),
                    () -> assertThat(orderProducts.getFirst().getOrder().getTotalAmount()).isEqualTo(105000)
            );
            assertAll(
                    () -> assertThat(album.getStockQuantity()).isEqualTo(8),
                    () -> assertThat(book.getStockQuantity()).isEqualTo(2),
                    () -> assertThat(movie.getStockQuantity()).isEqualTo(5)
            );

            //when 두 번째 호출
            orderService.updateOrder(order.getOrderNumber(), Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5));

            //then
            assertAll(
                    () -> then(orderRepository).should(times(2)).findWithAllExceptMember(anyString()),
                    () -> then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(any()),
                    () -> then(orderRepository).should().findByOrderNumber(any()),
                    () -> then(productService).should().findProducts(),
                    () -> then(orderProductJdbcRepository).should().saveAll(any())
            );

            assertAll(
                    () -> assertThat(album.getStockQuantity()).isEqualTo(8),
                    () -> assertThat(book.getStockQuantity()).isEqualTo(2),
                    () -> assertThat(movie.getStockQuantity()).isEqualTo(5)
            );
        }

        @Test
        void cancel_withoutPayment() {
            //given
            Order order = createOrder();

            given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));

            //when
            orderService.cancelOrder(order.getOrderNumber());

            //then
            then(orderRepository).should().findWithAllExceptMember(anyString());
            assertAll(
                    () -> assertThat(order.getOrderProducts())
                            .extracting("product.name", "count", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 3, 45000),
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 2, 30000),
                                    tuple("범죄도시", 4, 60000)),
                    () -> assertThat(order.getTotalAmount()).isEqualTo(135000)
            );
            assertAll(
                    () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED),
                    () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED),
                    () -> assertThat(order.getPayment()).isNull()
            );
            assertAll(
                    () -> assertThat(album.getStockQuantity()).isEqualTo(10),
                    () -> assertThat(book.getStockQuantity()).isEqualTo(7),
                    () -> assertThat(movie.getStockQuantity()).isEqualTo(5)
            );
        }

        @Test
        void cancel_withPayment() {
            //given
            Order order = createOrder();
            Payment.requestPayment(order);

            given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));

            //when
            orderService.cancelOrder(order.getOrderNumber());

            //then
            then(orderRepository).should().findWithAllExceptMember(anyString());
            assertAll(
                    () -> assertThat(order.getOrderProducts())
                            .extracting("product.name", "count", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 3, 45000),
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 2, 30000),
                                    tuple("범죄도시", 4, 60000)),
                    () -> assertThat(order.getTotalAmount()).isEqualTo(135000)
            );
            assertAll(
                    () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED),
                    () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED),
                    () -> assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED)
            );
            assertAll(
                    () -> assertThat(album.getStockQuantity()).isEqualTo(10),
                    () -> assertThat(book.getStockQuantity()).isEqualTo(7),
                    () -> assertThat(movie.getStockQuantity()).isEqualTo(5)
            );
        }

        @Test
        void cancel_idempotency() {
            //given
            Order order = createOrder();
            Payment.requestPayment(order);

            given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));

            //when 첫 번째 호출
            orderService.cancelOrder(order.getOrderNumber());

            //then
            then(orderRepository).should().findWithAllExceptMember(anyString());
            assertAll(
                    () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED),
                    () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED),
                    () -> assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED)
            );
            assertAll(
                    () -> assertThat(album.getStockQuantity()).isEqualTo(10),
                    () -> assertThat(book.getStockQuantity()).isEqualTo(7),
                    () -> assertThat(movie.getStockQuantity()).isEqualTo(5)
            );

            //when 두 번째 호출
            orderService.cancelOrder(order.getOrderNumber());

            then(orderRepository).should(times(2)).findWithAllExceptMember(anyString());
            assertAll(
                    () -> assertThat(order.getOrderProducts())
                            .extracting("product.name", "count", "orderPrice")
                            .containsExactlyInAnyOrder(
                                    tuple("BANG BANG", 3, 45000),
                                    tuple("자바 ORM 표준 JPA 프로그래밍", 2, 30000),
                                    tuple("범죄도시", 4, 60000)),
                    () -> assertThat(order.getTotalAmount()).isEqualTo(135000)
            );
            assertAll(
                    () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED),
                    () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED),
                    () -> assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED)
            );
            assertAll(
                    () -> assertThat(album.getStockQuantity()).isEqualTo(10),
                    () -> assertThat(book.getStockQuantity()).isEqualTo(7),
                    () -> assertThat(movie.getStockQuantity()).isEqualTo(5)
            );
        }

        @Test
        void delete_withoutPayment() {
            //given
            Order order = createOrder();
            order.cancel();

            given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.of(order));
            given(orderRepository.findWithDelivery(anyString())).willReturn(Optional.of(order));

            //when
            orderService.deleteOrder(order.getOrderNumber());

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithDeliveryPayment(anyString()),
                    () -> then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()),
                    () -> then(orderRepository).should().findWithDelivery(anyString()),
                    () -> then(orderRepository).should().delete(order)
            );
        }

        @Test
        void delete_withPayment() {
            //given
            Order order = createOrder();
            Payment.requestPayment(order);
            order.cancel();

            given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.of(order));
            given(orderRepository.findWithDelivery(anyString())).willReturn(Optional.of(order));

            //when
            orderService.deleteOrder(order.getOrderNumber());

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithDeliveryPayment(anyString()),
                    () -> then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()),
                    () -> then(orderRepository).should().findWithDelivery(anyString()),
                    () -> then(orderRepository).should().delete(order)
            );
        }

        @Test
        void changeDto() {
            Order order = createOrder();
            Payment.requestPayment(order);

            //when
            OrderRequest orderRequest = orderService.getOrderRequest(order);
            OrderResponse orderResponse = orderService.getOrderResponse(order);

            //then
            assertThat(orderRequest)
                    .extracting("orderNumber", "memberLoginId", "paymentId")
                    .containsExactlyInAnyOrder(order.getOrderNumber(), "id_A", order.getPayment().getPaymentId());
            assertThat(orderResponse)
                    .extracting("paymentDate", "shippedDate", "deliveredDate")
                    .containsOnlyNulls();
            assertThat(orderRequest.getOrderProducts())
                    .usingRecursiveComparison()
                    .isEqualTo(orderResponse.getOrderProducts());
        }
    }

    @Nested
    class FailureCase {

        @Test
        void order_productMapIsEmpty() {
            //given
            Map<String, Integer> emptyProductMap = new HashMap<>();

            //when
            assertThatThrownBy(() -> orderService.order("id_A", emptyProductMap))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문할 상품이 없습니다.");

            //then
            assertAll(
                    () -> then(memberService).should(never()).findMemberByLoginId(any()),
                    () -> then(productService).should(never()).findProducts(),
                    () -> then(orderRepository).should(never()).save(any()),
                    () -> then(orderProductJdbcRepository).should(never()).saveAll(any())
            );
        }

        @Test
        void order_memberNotFound() {
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
        void order_productMapContainsNullEntry(String key, Integer value, String message) {
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
        void order_productNotFound() {
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
        void order_failedSaveAll() {
            //given
            Map<String, Integer> productMap = Map.of("BANG BANG", 3, "자바 ORM 표준 JPA 프로그래밍", 2, "범죄도시", 4);

            given(memberService.findMemberByLoginId(anyString())).willReturn(member);
            given(productService.findProducts()).willReturn(List.of(album, book, movie));
            willThrow(new RuntimeException("JDBC Batch INSERT Failed")).given(orderProductJdbcRepository).saveAll(anyList());

            //when
            assertThatThrownBy(() -> orderService.order("id_A", productMap))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("JDBC Batch INSERT Failed");

            //then
            assertAll(
                    () -> then(memberService).should().findMemberByLoginId(anyString()),
                    () -> then(productService).should().findProducts(),
                    () -> then(orderRepository).should().save(any()),
                    () -> then(orderProductJdbcRepository).should().saveAll(anyList())
            );
        }

        @Test
        void find_orderNotFound() {
            //given
            given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> orderService.findOrder(wrongOrderNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 주문입니다.");

            //then
            then(orderRepository).should().findByOrderNumber(anyString());
        }

        @Test
        void update_productMapIsEmpty() {
            //given
            Order order = createOrder();

            Map<String, Integer> emptyProductMap = new HashMap<>();

            //when
            assertThatThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), emptyProductMap))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문을 수정할 상품이 없습니다.");

            //then
            assertAll(
                    () -> then(orderRepository).should(never()).findWithAllExceptMember(any()),
                    () -> then(orderProductJdbcRepository).should(never()).deleteOrderProductsByOrderId(any()),
                    () -> then(orderRepository).should(never()).findByOrderNumber(any()),
                    () -> then(productService).should(never()).findProducts(),
                    () -> then(orderProductJdbcRepository).should(never()).saveAll(any())
            );
        }

        @Test
        void update_orderNotFound() {
            //given
            Map<String, Integer> newProductMap = Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5);

            given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> orderService.updateOrder(wrongOrderNumber, newProductMap))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 주문입니다.");

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithAllExceptMember(anyString()),
                    () -> then(orderProductJdbcRepository).should(never()).deleteOrderProductsByOrderId(any()),
                    () -> then(orderRepository).should(never()).findByOrderNumber(any()),
                    () -> then(productService).should(never()).findProducts(),
                    () -> then(orderProductJdbcRepository).should(never()).saveAll(any())
            );
        }

        @Test
        void update_orderIdIsNull() {
            //given
            Order order = createNotSavedOrder();

            Map<String, Integer> newProductMap = Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5);

            given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));

            //when
            assertThatThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), newProductMap))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("식별자가 없는 잘못된 주문입니다.");

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithAllExceptMember(anyString()),
                    () -> then(orderProductJdbcRepository).should(never()).deleteOrderProductsByOrderId(any()),
                    () -> then(orderRepository).should(never()).findByOrderNumber(any()),
                    () -> then(productService).should(never()).findProducts(),
                    () -> then(orderProductJdbcRepository).should(never()).saveAll(any())
            );
        }

        @Test
        void update_failedDeleteOrderProducts() {
            //given
            Order order = createOrder();

            Map<String, Integer> newProductMap = Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5);

            given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
            willThrow(new RuntimeException("JDBC DELETE Failed")).given(orderProductJdbcRepository).deleteOrderProductsByOrderId(anyLong());

            //when
            assertThatThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), newProductMap))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("JDBC DELETE Failed");

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithAllExceptMember(anyString()),
                    () -> then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()),
                    () -> then(orderRepository).should(never()).findByOrderNumber(any()),
                    () -> then(productService).should(never()).findProducts(),
                    () -> then(orderProductJdbcRepository).should(never()).saveAll(any())
            );
        }

        @ParameterizedTest
        @MethodSource("keyValueProvider")
        void update_productMapContainsNullEntry(String key, Integer value, String message) {
            //given
            Order order = createOrder();

            Map<String, Integer> newProductMap = new HashMap<>();
            newProductMap.put(key, value);

            given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
            given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
            given(productService.findProducts()).willReturn(List.of(album, book, movie));

            //when
            assertThatThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), newProductMap))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(message);

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithAllExceptMember(anyString()),
                    () -> then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()),
                    () -> then(orderRepository).should().findByOrderNumber(anyString()),
                    () -> then(productService).should().findProducts(),
                    () -> then(orderProductJdbcRepository).should(never()).saveAll(any())
            );
        }

        @Test
        void update_productNotFound() {
            //given
            Order order = createOrder();

            Map<String, Integer> newProductMap = Map.of("타임 캡슐", 3);

            given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
            given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
            given(productService.findProducts()).willReturn(List.of(album, book, movie));

            //when
            assertThatThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), newProductMap))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 상품입니다. name: " + "타임 캡슐");

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithAllExceptMember(anyString()),
                    () -> then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()),
                    () -> then(orderRepository).should().findByOrderNumber(anyString()),
                    () -> then(productService).should().findProducts(),
                    () -> then(orderProductJdbcRepository).should(never()).saveAll(any())
            );
        }

        @Test
        void update_failedSaveAll() {
            //given
            Order order = createOrder();

            Map<String, Integer> newProductMap = Map.of("BANG BANG", 2, "자바 ORM 표준 JPA 프로그래밍", 5);

            given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.of(order));
            given(orderRepository.findByOrderNumber(anyString())).willReturn(Optional.of(order));
            given(productService.findProducts()).willReturn(List.of(album, book, movie));
            willThrow(new RuntimeException("JDBC Batch INSERT Failed")).given(orderProductJdbcRepository).saveAll(anyList());

            //when
            assertThatThrownBy(() -> orderService.updateOrder(order.getOrderNumber(), newProductMap))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("JDBC Batch INSERT Failed");

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithAllExceptMember(anyString()),
                    () -> then(orderProductJdbcRepository).should().deleteOrderProductsByOrderId(anyLong()),
                    () -> then(orderRepository).should().findByOrderNumber(anyString()),
                    () -> then(productService).should().findProducts(),
                    () -> then(orderProductJdbcRepository).should().saveAll(anyList())
            );
        }

        @Test
        void cancel_orderNotFound() {
            //given
            given(orderRepository.findWithAllExceptMember(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> orderService.cancelOrder(wrongOrderNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 주문입니다.");

            //then
            then(orderRepository).should().findWithAllExceptMember(anyString());
        }

        @Test
        void delete_orderNotFound() {
            //given
            given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.empty());

            //when
            assertThatThrownBy(() -> orderService.deleteOrder(wrongOrderNumber))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 주문입니다.");

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithDeliveryPayment(anyString()),
                    () -> then(orderProductJdbcRepository).should(never()).softDeleteOrderProductsByOrderId(any()),
                    () -> then(orderRepository).should(never()).findWithDelivery(any()),
                    () -> then(orderRepository).should(never()).delete(any())
            );
        }

        @Test
        void delete_failedSoftDeleteOrderProducts() {
            //given
            Order order = createOrder();
            order.cancel();

            given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.of(order));
            willThrow(new RuntimeException("JDBC Soft DELETE Failed")).given(orderProductJdbcRepository).softDeleteOrderProductsByOrderId(anyLong());

            //when
            assertThatThrownBy(() -> orderService.deleteOrder(order.getOrderNumber()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("JDBC Soft DELETE Failed");

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithDeliveryPayment(anyString()),
                    () -> then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()),
                    () -> then(orderRepository).should(never()).findWithDelivery(any()),
                    () -> then(orderRepository).should(never()).delete(any())
            );
        }

        @Test
        void delete_failedDeleteOrder() {
            //given
            Order order = createOrder();
            order.cancel();

            given(orderRepository.findWithDeliveryPayment(anyString())).willReturn(Optional.of(order));
            given(orderRepository.findWithDelivery(anyString())).willReturn(Optional.of(order));
            willThrow(new RuntimeException("DELETE Failed")).given(orderRepository).delete(any());

            //when
            assertThatThrownBy(() -> orderService.deleteOrder(order.getOrderNumber()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DELETE Failed");

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithDeliveryPayment(anyString()),
                    () -> then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()),
                    () -> then(orderRepository).should().findWithDelivery(any()),
                    () -> then(orderRepository).should().delete(any())
            );
        }

        @Test
        void delete_alreadyDeleted() {
            //given
            Order order = createOrder();
            Payment.requestPayment(order);
            order.cancel();

            given(orderRepository.findWithDeliveryPayment(anyString()))
                    .willReturn(Optional.of(order))
                    .willReturn(Optional.empty());
            given(orderRepository.findWithDelivery(anyString())).willReturn(Optional.of(order));

            //when 첫 번째 호출
            orderService.deleteOrder(order.getOrderNumber());

            //then
            assertAll(
                    () -> then(orderRepository).should().findWithDeliveryPayment(anyString()),
                    () -> then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(anyLong()),
                    () -> then(orderRepository).should().findWithDelivery(anyString()),
                    () -> then(orderRepository).should().delete(order)
            );

            //when 두 번째 호출
            assertThatThrownBy(() -> orderService.deleteOrder(order.getOrderNumber()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 주문입니다.");

            //then
            assertAll(
                    () -> then(orderRepository).should(times(2)).findWithDeliveryPayment(anyString()),
                    () -> then(orderProductJdbcRepository).should().softDeleteOrderProductsByOrderId(any()),
                    () -> then(orderRepository).should().findWithDelivery(any()),
                    () -> then(orderRepository).should().delete(any())
            );
        }

        static Stream<Arguments> keyValueProvider() {
            return Stream.of(
                    argumentSet("키 null", null, 3, "존재하지 않는 상품입니다. name: " + "null"),
                    argumentSet("값 null", "BANG BANG", null, "수량이 없습니다."),
                    argumentSet("키, 값 모두 null", null, null, "존재하지 않는 상품입니다. name: " + "null")
            );
        }

        private Order createNotSavedOrder() {
            Delivery delivery = new Delivery(member);

            OrderProduct orderProduct1 = OrderProduct.createOrderProduct(album, 3);
            OrderProduct orderProduct2 = OrderProduct.createOrderProduct(book, 2);
            OrderProduct orderProduct3 = OrderProduct.createOrderProduct(movie, 4);

            return Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2, orderProduct3));
        }
    }

    private Order createOrder() {
        Delivery delivery = new Delivery(member);

        OrderProduct orderProduct1 = OrderProduct.createOrderProduct(album, 3);
        OrderProduct orderProduct2 = OrderProduct.createOrderProduct(book, 2);
        OrderProduct orderProduct3 = OrderProduct.createOrderProduct(movie, 4);

        Order order = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2, orderProduct3));

        ReflectionTestUtils.setField(order, "id", 1L);

        return order;
    }
}