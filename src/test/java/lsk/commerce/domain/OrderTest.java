package lsk.commerce.domain;

import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class OrderTest {

    Member member;
    Delivery delivery;
    Album album;
    Book book;
    Movie movie;
    OrderProduct orderProduct1;
    OrderProduct orderProduct2;
    Order order;

    @BeforeEach
    void beforeEach() {
        member = Member.builder().city("Seoul").street("Gangnam").zipcode("01234").build();
        delivery = new Delivery(member);

        album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).build();
        book = Book.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(7).build();
        movie = Movie.builder().name("범죄도시").price(15000).stockQuantity(5).build();

        orderProduct1 = OrderProduct.createOrderProduct(album, 5);
        orderProduct2 = OrderProduct.createOrderProduct(book, 3);

        order = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
    }

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                Order createdOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

                //then
                assertAll(
                        () -> assertThat(createdOrder.getMember().getAddress()).isEqualTo(createdOrder.getDelivery().getAddress()),
                        () -> assertThat(createdOrder.getOrderProducts())
                                .isNotEmpty()
                                .extracting("order", "product", "orderPrice", "count")
                                .containsExactlyInAnyOrder(tuple(createdOrder, album, 75000, 5), tuple(createdOrder, book, 45000, 3)),
                        () -> assertThat(createdOrder.getTotalAmount()).isEqualTo(120000)
                );
                assertAll(
                        () -> assertThat(album.getStockQuantity()).isEqualTo(5),
                        () -> assertThat(book.getStockQuantity()).isEqualTo(4)
                );
            }
        }

        @Nested
        class FailureCase {

            @Test
            void addressNull() {
                //given
                Member nullAddressMember = new Member();
                Delivery nullAddressDelivery = new Delivery();

                //when
                assertAll(
                        () -> assertThatThrownBy(() -> Order.createOrder(nullAddressMember, delivery, List.of(orderProduct1, orderProduct2)))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("배송될 주소가 없습니다."),
                        () -> assertThatThrownBy(() -> Order.createOrder(member, nullAddressDelivery, List.of(orderProduct1, orderProduct2)))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("배송될 주소가 없습니다.")
                );
            }
        }
    }

    @Nested
    class Clear {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                order.clearOrderProduct();

                //then
                assertAll(
                        () -> assertThat(order.getOrderProducts()).isEmpty(),
                        () -> assertThat(order.getTotalAmount()).isEqualTo(0)
                );
                assertAll(
                        () -> assertThat(album.getStockQuantity()).isEqualTo(10),
                        () -> assertThat(book.getStockQuantity()).isEqualTo(7)
                );
            }

            @Test
            void idempotency() {
                //when 첫 번째 호출
                order.clearOrderProduct();

                //then
                assertAll(
                        () -> assertThat(order.getOrderProducts()).isEmpty(),
                        () -> assertThat(order.getTotalAmount()).isEqualTo(0)
                );
                assertAll(
                        () -> assertThat(album.getStockQuantity()).isEqualTo(10),
                        () -> assertThat(book.getStockQuantity()).isEqualTo(7)
                );

                //when 두 번째 호출
                order.clearOrderProduct();

                //then
                assertAll(
                        () -> assertThat(order.getOrderProducts()).isEmpty(),
                        () -> assertThat(order.getTotalAmount()).isEqualTo(0)
                );
                assertAll(
                        () -> assertThat(album.getStockQuantity()).isEqualTo(10),
                        () -> assertThat(book.getStockQuantity()).isEqualTo(7)
                );
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("orderStatusProvider")
            void orderStatusIsNotCreated(OrderStatus orderStatus) {
                //given
                ReflectionTestUtils.setField(order, "orderStatus", orderStatus);

                //when
                assertThatThrownBy(() -> order.clearOrderProduct())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("주문 생성 상태가 아니어서 주문 상품을 비울 수 없습니다. OrderStatus: " + orderStatus);
            }

            @ParameterizedTest
            @MethodSource("paymentStatusProvider")
            void paymentStatusIsNotPending(PaymentStatus paymentStatus) {
                //given
                Payment.requestPayment(order);

                ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", paymentStatus);

                //when
                assertThatThrownBy(() -> order.clearOrderProduct())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("결제 대기 상태가 아니어서 주문 상품을 비울 수 없습니다. PaymentStatus: " + paymentStatus);
            }

            @ParameterizedTest
            @MethodSource("deliveryStatusProvider")
            void deliveryStatusIsNotWaiting(DeliveryStatus deliveryStatus) {
                //given
                ReflectionTestUtils.setField(delivery, "deliveryStatus", deliveryStatus);

                //when
                assertThatThrownBy(() -> order.clearOrderProduct())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("배송 대기 상태가 아니어서 주문 상품을 비울 수 없습니다. DeliveryStatus: " + deliveryStatus);
            }

            static Stream<Arguments> orderStatusProvider() {
                return Stream.of(
                        argumentSet("OrderStatus: CANCELED", OrderStatus.CANCELED),
                        argumentSet("OrderStatus: PAID", OrderStatus.PAID),
                        argumentSet("OrderStatus: DELIVERED", OrderStatus.DELIVERED)
                );
            }

            static Stream<Arguments> paymentStatusProvider() {
                return Stream.of(
                        argumentSet("PaymentStatus: CANCELED", PaymentStatus.CANCELED),
                        argumentSet("PaymentStatus: FAILED", PaymentStatus.FAILED),
                        argumentSet("PaymentStatus: COMPLETED", PaymentStatus.COMPLETED)
                );
            }

            static Stream<Arguments> deliveryStatusProvider() {
                return Stream.of(
                        argumentSet("DeliveryStatus: CANCELED", DeliveryStatus.CANCELED),
                        argumentSet("DeliveryStatus: PREPARING", DeliveryStatus.PREPARING),
                        argumentSet("DeliveryStatus: SHIPPED", DeliveryStatus.SHIPPED),
                        argumentSet("DeliveryStatus: DELIVERED", DeliveryStatus.DELIVERED)
                );
            }
        }
    }

    @Nested
    class GetOrderProductAsMap {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                Map<String, Integer> orderProductsAsMap = order.getOrderProductsAsMap();

                //then
                assertThat(orderProductsAsMap)
                        .hasSize(2)
                        .containsOnly(entry("BANG BANG", 5), entry("자바 ORM 표준 JPA 프로그래밍", 3));
            }
        }
    }

    @Nested
    class Update {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                order.clearOrderProduct();

                OrderProduct orderProduct3 = OrderProduct.createOrderProduct(movie, 2);
                OrderProduct orderProduct4 = OrderProduct.createOrderProduct(album, 2);

                //when
                order.updateOrder(List.of(orderProduct3, orderProduct4));

                //then
                assertAll(
                        () -> assertThat(order.getOrderProducts())
                                .isNotEmpty()
                                .extracting("product", "orderPrice", "count")
                                .containsExactlyInAnyOrder(tuple(movie, 30000, 2), tuple(album, 30000, 2)),
                        () -> assertThat(order.getTotalAmount()).isEqualTo(60000)
                );
                assertAll(
                        () -> assertThat(album.getStockQuantity()).isEqualTo(8),
                        () -> assertThat(movie.getStockQuantity()).isEqualTo(3)
                );
            }
        }
    }

    @Nested
    class Cancel {

        @Nested
        class SuccessCase {

            @Test
            void withoutPayment() {
                //when
                order.cancel();

                //then
                assertAll(
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED),
                        () -> assertThat(order.getPayment()).isNull()
                );
                assertAll(
                        () -> assertThat(album.getStockQuantity()).isEqualTo(10),
                        () -> assertThat(book.getStockQuantity()).isEqualTo(7)
                );
            }

            @Test
            void withPayment() {
                //given
                Payment payment = new Payment();

                ReflectionTestUtils.setField(payment, "paymentStatus", PaymentStatus.PENDING);
                ReflectionTestUtils.setField(order, "payment", payment);

                //when
                order.cancel();

                //then
                assertAll(
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED),
                        () -> assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED)
                );
                assertAll(
                        () -> assertThat(album.getStockQuantity()).isEqualTo(10),
                        () -> assertThat(book.getStockQuantity()).isEqualTo(7)
                );
            }

            @Test
            void idempotency() {
                //when 첫 번째 호출
                order.cancel();

                //then
                assertAll(
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED),
                        () -> assertThat(order.getPayment()).isNull()
                );
                assertAll(
                        () -> assertThat(album.getStockQuantity()).isEqualTo(10),
                        () -> assertThat(book.getStockQuantity()).isEqualTo(7)
                );

                //when 두 번째 호출
                order.cancel();

                //then
                assertAll(
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED),
                        () -> assertThat(order.getPayment()).isNull()
                );
                assertAll(
                        () -> assertThat(album.getStockQuantity()).isEqualTo(10),
                        () -> assertThat(book.getStockQuantity()).isEqualTo(7)
                );
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("orderStatusProvider")
            void orderStatusIsPaid(OrderStatus orderStatus) {
                //given
                ReflectionTestUtils.setField(order, "orderStatus", orderStatus);

                //when
                assertThatThrownBy(() -> order.cancel())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("결제 완료된 주문이어서 취소할 수 없습니다. OrderStatus: " + orderStatus);
            }

            @Test
            void paymentStatusIsCompleted() {
                //given
                Payment.requestPayment(order);

                ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", PaymentStatus.COMPLETED);

                //when
                assertThatThrownBy(() -> order.cancel())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("결제 완료돼서 취소할 수 없습니다.");
            }

            @ParameterizedTest
            @MethodSource("deliveryStatusProvider")
            void deliveryStatusIsNotWaiting(DeliveryStatus deliveryStatus) {
                //given
                ReflectionTestUtils.setField(delivery, "deliveryStatus", deliveryStatus);

                //when
                assertThatThrownBy(() -> order.cancel())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("배송 대기 상태가 아니여서 취소할 수 없습니다. DeliveryStatus: " + deliveryStatus);
            }

            static Stream<Arguments> orderStatusProvider() {
                return Stream.of(
                        argumentSet("OrderStatus: PAID", OrderStatus.PAID),
                        argumentSet("OrderStatus: DELIVERED", OrderStatus.DELIVERED)
                );
            }

            static Stream<Arguments> deliveryStatusProvider() {
                return Stream.of(
                        argumentSet("DeliveryStatus: PREPARING", DeliveryStatus.PREPARING),
                        argumentSet("DeliveryStatus: SHIPPED", DeliveryStatus.SHIPPED),
                        argumentSet("DeliveryStatus: DELIVERED", DeliveryStatus.DELIVERED)
                );
            }
        }
    }

    @Nested
    class CompletePaid {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Payment payment = new Payment();

                ReflectionTestUtils.setField(payment, "paymentStatus", PaymentStatus.COMPLETED);
                ReflectionTestUtils.setField(order, "payment", payment);

                //when
                order.completePaid();

                //then
                assertAll(
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID),
                        () -> assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.PREPARING)
                );
            }

            @Test
            void idempotency() {
                //given
                Payment payment = new Payment();

                ReflectionTestUtils.setField(payment, "paymentStatus", PaymentStatus.COMPLETED);
                ReflectionTestUtils.setField(order, "payment", payment);

                //when 첫 번째 호출
                order.completePaid();

                //then
                assertAll(
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID),
                        () -> assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.PREPARING)
                );

                //when 두 번째 호출
                order.completePaid();

                //then
                assertAll(
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID),
                        () -> assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.PREPARING)
                );
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("paymentStatusProvider")
            void paymentStatusIsNotCompleted(PaymentStatus paymentStatus) {
                //given
                Payment.requestPayment(order);

                ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", paymentStatus);

                //when
                assertThatThrownBy(() -> order.completePaid())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("결제가 완료되지 않았습니다. PaymentStatus: " + paymentStatus);
            }

            @ParameterizedTest
            @MethodSource("orderStatusProvider")
            void orderStatusIsNotCreated(OrderStatus orderStatus) {
                //given
                Payment.requestPayment(order);

                ReflectionTestUtils.setField(order, "orderStatus", orderStatus);
                ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", PaymentStatus.COMPLETED);

                //when
                assertThatThrownBy(() -> order.completePaid())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("결제 완료할 수 없는 상태입니다. OrderStatus: " + orderStatus);
            }

            @ParameterizedTest
            @MethodSource("deliveryStatusProvider")
            void deliveryStatusIsNotWaiting(DeliveryStatus deliveryStatus) {
                //given
                Payment.requestPayment(order);

                ReflectionTestUtils.setField(delivery, "deliveryStatus", deliveryStatus);
                ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", PaymentStatus.COMPLETED);

                //when
                assertThatThrownBy(() -> order.completePaid())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("결제 완료할 수 없는 상태입니다. DeliveryStatus: " + deliveryStatus);
            }

            static Stream<Arguments> paymentStatusProvider() {
                return Stream.of(
                        argumentSet("PaymentStatus: PENDING", PaymentStatus.PENDING),
                        argumentSet("PaymentStatus: CANCELED", PaymentStatus.CANCELED),
                        argumentSet("PaymentStatus: FAILED", PaymentStatus.FAILED)
                );
            }

            static Stream<Arguments> orderStatusProvider() {
                return Stream.of(
                        argumentSet("OrderStatus: CANCELED", OrderStatus.CANCELED),
                        argumentSet("OrderStatus: DELIVERED", OrderStatus.DELIVERED)
                );
            }

            static Stream<Arguments> deliveryStatusProvider() {
                return Stream.of(
                        argumentSet("DeliveryStatus: CANCELED", DeliveryStatus.CANCELED),
                        argumentSet("DeliveryStatus: PREPARING", DeliveryStatus.PREPARING),
                        argumentSet("DeliveryStatus: SHIPPED", DeliveryStatus.SHIPPED),
                        argumentSet("DeliveryStatus: DELIVERED", DeliveryStatus.DELIVERED)
                );
            }
        }
    }

    @Nested
    class ValidateDeletable {

        @Nested
        class SuccessCase {

            @Test
            void withoutPayment() {
                //given
                order.cancel();

                //when
                assertDoesNotThrow(() -> order.validateDeletable());
            }

            @Test
            void withPayment() {
                //given
                Payment payment = new Payment();

                ReflectionTestUtils.setField(payment, "paymentStatus", PaymentStatus.PENDING);
                ReflectionTestUtils.setField(order, "payment", payment);

                order.cancel();

                //when
                assertDoesNotThrow(() -> order.validateDeletable());
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderStatusIsCreated() {
                //given
                Order createdOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

                ReflectionTestUtils.setField(createdOrder, "orderStatus", OrderStatus.CREATED);

                //when
                assertThatThrownBy(() -> createdOrder.validateDeletable())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("주문을 취소해야 삭제할 수 있습니다.");
            }

            @Test
            void orderStatusIsPaid() {
                //given
                Order paidOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
                Payment.requestPayment(paidOrder);

                ReflectionTestUtils.setField(paidOrder, "orderStatus", OrderStatus.PAID);

                //when
                assertThatThrownBy(() -> paidOrder.validateDeletable())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("배송이 완료돼야 삭제할 수 있습니다.");
            }

            @Test
            void deliveryStatusIsWaiting() {
                //given
                Order waitingOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

                ReflectionTestUtils.setField(waitingOrder, "orderStatus", OrderStatus.CANCELED);
                ReflectionTestUtils.setField(waitingOrder.getDelivery(), "deliveryStatus", DeliveryStatus.WAITING);

                //when
                assertThatThrownBy(() -> waitingOrder.validateDeletable())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("주문을 취소해야 삭제할 수 있습니다. DeliveryStatus: " + waitingOrder.getDelivery().getDeliveryStatus());
            }

            @ParameterizedTest
            @MethodSource("deliveryStatusProvider")
            void deliveryStatusIsPreparingOrShipped(DeliveryStatus deliveryStatus) {
                //given
                Payment.requestPayment(order);

                ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.DELIVERED);
                ReflectionTestUtils.setField(delivery, "deliveryStatus", deliveryStatus);

                //when
                assertThatThrownBy(() -> order.validateDeletable())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("배송이 완료돼야 삭제할 수 있습니다. DeliveryStatus: " + deliveryStatus);
            }

            @ParameterizedTest
            @MethodSource("paymentStatusProvider")
            void paymentStatusIsPendingOrFailed(PaymentStatus paymentStatus) {
                //given
                Payment.requestPayment(order);

                ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.CANCELED);
                ReflectionTestUtils.setField(delivery, "deliveryStatus", DeliveryStatus.CANCELED);
                ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", paymentStatus);

                //when
                assertThatThrownBy(() -> order.validateDeletable())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("주문을 취소해야 삭제할 수 있습니다. PaymentStatus: " + paymentStatus);
            }

            static Stream<Arguments> deliveryStatusProvider() {
                return Stream.of(
                        argumentSet("DeliveryStatus: PREPARING", DeliveryStatus.PREPARING),
                        argumentSet("DeliveryStatus: SHIPPED", DeliveryStatus.SHIPPED)
                );
            }

            static Stream<Arguments> paymentStatusProvider() {
                return Stream.of(
                        argumentSet("PaymentStatus: PENDING", PaymentStatus.PENDING),
                        argumentSet("PaymentStatus: FAILED", PaymentStatus.FAILED)
                );
            }
        }
    }
}