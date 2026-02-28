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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.params.provider.Arguments.*;

class PaymentTest {

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
    class Request {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                Payment.requestPayment(order);

                //then
                assertThat(order.getPayment())
                        .isNotNull()
                        .extracting("order", "paymentAmount", "paymentDate", "paymentStatus")
                        .containsExactly(order, order.getTotalAmount(), null, PaymentStatus.PENDING);
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderNull() {
                //when
                assertThatThrownBy(() -> Payment.requestPayment(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("주문 정보가 없습니다.");
            }

            @Test
            void orderStatusIsCanceled() {
                //given
                Order canceldOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

                ReflectionTestUtils.setField(canceldOrder, "orderStatus", OrderStatus.CANCELED);

                //when
                assertThatThrownBy(() -> Payment.requestPayment(canceldOrder))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("취소된 주문은 결제할 수 없습니다. OrderStatus: " + canceldOrder.getOrderStatus());
            }

            @ParameterizedTest
            @MethodSource("lsk.commerce.domain.PaymentTest#orderStatusProvider")
            void orderStatusIsPaidOrDelivered(OrderStatus orderStatus) {
                //given
                ReflectionTestUtils.setField(order, "orderStatus", orderStatus);

                //when
                assertThatThrownBy(() -> Payment.requestPayment(order))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 결제 완료된 주문입니다. OrderStatus: " + orderStatus);
            }

            @Test
            void deliveryStatusIsCanceled() {
                //given
                Order canceledOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

                ReflectionTestUtils.setField(canceledOrder.getDelivery(), "deliveryStatus", DeliveryStatus.CANCELED);

                //when
                assertThatThrownBy(() -> Payment.requestPayment(canceledOrder))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("취소된 주문은 결제할 수 없습니다. DeliveryStatus: " + canceledOrder.getDelivery().getDeliveryStatus());
            }

            @ParameterizedTest
            @MethodSource("lsk.commerce.domain.PaymentTest#deliveryStatusProvider")
            void deliveryStatusIsNotWaiting(DeliveryStatus deliveryStatus) {
                //given
                ReflectionTestUtils.setField(delivery, "deliveryStatus", deliveryStatus);

                //when
                assertThatThrownBy(() -> Payment.requestPayment(order))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 결제 완료된 주문입니다. DeliveryStatus: " + deliveryStatus);
            }

            @Test
            void duplicateRequest() {
                //when 첫 번째 호출
                Payment.requestPayment(order);

                //then
                assertThat(order.getPayment())
                        .isNotNull()
                        .extracting("order", "paymentAmount", "paymentDate", "paymentStatus")
                        .containsExactly(order, order.getTotalAmount(), null, PaymentStatus.PENDING);

                //when 두 번째 호출
                assertThatThrownBy(() -> Payment.requestPayment(order))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 결제 정보가 있습니다.");
            }
        }
    }

    @Nested
    class Complete {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Payment.requestPayment(order);

                //when
                order.getPayment().complete(LocalDateTime.now());

                //then
                assertAll(
                        () -> assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED),
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.WAITING)
                );
            }

            @Test
            void paymentStatusIsFailed() {
                //given
                Payment.requestPayment(order);

                ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", PaymentStatus.FAILED);

                //when
                order.getPayment().complete(LocalDateTime.now());

                //then
                assertAll(
                        () -> assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED),
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.WAITING)
                );
            }
        }

        @Nested
        class FailureCase {

            @Test
            void paymentStatusIsCanceled() {
                //given
                Order canceledOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
                Payment.requestPayment(canceledOrder);

                ReflectionTestUtils.setField(canceledOrder.getPayment(), "paymentStatus", PaymentStatus.CANCELED);

                //when
                assertThatThrownBy(() -> canceledOrder.getPayment().complete(LocalDateTime.now()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("취소된 주문은 결제할 수 없습니다.");
            }

            @Test
            void orderStatusIsCanceled() {
                //given
                Order canceledOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
                Payment.requestPayment(canceledOrder);

                ReflectionTestUtils.setField(canceledOrder, "orderStatus", OrderStatus.CANCELED);

                //when
                assertThatThrownBy(() -> canceledOrder.getPayment().complete(LocalDateTime.now()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("취소된 주문은 결제할 수 없습니다. OrderStatus: " + canceledOrder.getOrderStatus());
            }

            @ParameterizedTest
            @MethodSource("lsk.commerce.domain.PaymentTest#orderStatusProvider")
            void orderStatusIsPaidOrDelivered(OrderStatus orderStatus) {
                //given
                Payment.requestPayment(order);

                ReflectionTestUtils.setField(order, "orderStatus", orderStatus);

                //when
                assertThatThrownBy(() -> order.getPayment().complete(LocalDateTime.now()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 결제 완료된 주문입니다. OrderStatus: " + orderStatus);
            }

            @Test
            void deliveryStatusIsCanceled() {
                //given
                Order canceledOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
                Payment.requestPayment(canceledOrder);

                ReflectionTestUtils.setField(canceledOrder.getDelivery(), "deliveryStatus", DeliveryStatus.CANCELED);

                //when
                assertThatThrownBy(() -> canceledOrder.getPayment().complete(LocalDateTime.now()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("취소된 주문은 결제할 수 없습니다. DeliveryStatus: " + canceledOrder.getDelivery().getDeliveryStatus());
            }

            @ParameterizedTest
            @MethodSource("lsk.commerce.domain.PaymentTest#deliveryStatusProvider")
            void deliveryStatusIsNotWaiting(DeliveryStatus deliveryStatus) {
                //given
                Payment.requestPayment(order);

                ReflectionTestUtils.setField(order.getDelivery(), "deliveryStatus", deliveryStatus);

                //when
                assertThatThrownBy(() -> order.getPayment().complete(LocalDateTime.now()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 결제 완료된 주문입니다. DeliveryStatus: " + deliveryStatus);
            }

            @Test
            void duplicateComplete() {
                //given
                Payment.requestPayment(order);

                //when 첫 번째 호출
                order.getPayment().complete(LocalDateTime.now());

                //then
                assertAll(
                        () -> assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED),
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.WAITING)
                );

                //when 두 번째 호출
                assertThatThrownBy(() -> order.getPayment().complete(LocalDateTime.now()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 결제 완료된 주문입니다.");
            }
        }
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