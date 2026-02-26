package lsk.commerce.domain;

import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    Member member;
    Delivery delivery1;
    Delivery delivery2;
    Delivery delivery3;
    Delivery delivery4;
    Album album;
    Book book;
    Movie movie;
    OrderProduct orderProduct1;
    OrderProduct orderProduct2;
    Order order;

    @BeforeEach
    void beforeEach() {
        member = Member.builder().city("Seoul").street("Gangnam").zipcode("01234").build();
        delivery1 = new Delivery(member);
        delivery2 = new Delivery(member);
        delivery3 = new Delivery(member);
        delivery4 = new Delivery(member);
        album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).build();
        book = Book.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(7).build();
        movie = Movie.builder().name("범죄도시").price(15000).stockQuantity(5).build();
        orderProduct1 = OrderProduct.createOrderProduct(album, 5);
        orderProduct2 = OrderProduct.createOrderProduct(book, 3);
        order = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
    }

    @Nested
    class SuccessCase {

        @Test
        void request() {
            //when
            Payment.requestPayment(order);

            //then
            assertThat(order.getPayment())
                    .isNotNull()
                    .extracting("order", "paymentAmount", "paymentDate", "paymentStatus")
                    .containsExactly(order, order.getTotalAmount(), null, PaymentStatus.PENDING);
        }

        @Test
        void complete() {
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
        void complete_fromFailed() {
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
        void request_orderNull() {
            //when
            assertThatThrownBy(() -> Payment.requestPayment(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 정보가 없습니다.");
        }

        @Test
        void request_orderStatusIsCanceled() {
            //given
            Order canceldOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(canceldOrder, "orderStatus", OrderStatus.CANCELED);

            //when
            assertThatThrownBy(() -> Payment.requestPayment(canceldOrder))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("취소된 주문은 결제할 수 없습니다. OrderStatus: " + canceldOrder.getOrderStatus());
        }

        @Test
        void request_orderStatusIsPaidOrDelivered() {
            //given
            Order paidOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order deliveryedOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(paidOrder, "orderStatus", OrderStatus.PAID);
            ReflectionTestUtils.setField(deliveryedOrder, "orderStatus", OrderStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> Payment.requestPayment(paidOrder))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("이미 결제 완료된 주문입니다. OrderStatus: " + paidOrder.getOrderStatus()),
                    () -> assertThatThrownBy(() -> Payment.requestPayment(deliveryedOrder))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("이미 결제 완료된 주문입니다. OrderStatus: " + deliveryedOrder.getOrderStatus())
            );
        }

        @Test
        void request_deliveryStatusIsCanceled() {
            //given
            Order canceledOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(canceledOrder.getDelivery(), "deliveryStatus", DeliveryStatus.CANCELED);

            //when
            assertThatThrownBy(() -> Payment.requestPayment(canceledOrder))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("취소된 주문은 결제할 수 없습니다. DeliveryStatus: " + canceledOrder.getDelivery().getDeliveryStatus());
        }

        @Test
        void request_deliveryStatusIsNotWaiting() {
            //given
            Order preparingOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order shippedOrder = Order.createOrder(member, delivery2, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery3, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(preparingOrder.getDelivery(), "deliveryStatus", DeliveryStatus.PREPARING);
            ReflectionTestUtils.setField(shippedOrder.getDelivery(), "deliveryStatus", DeliveryStatus.SHIPPED);
            ReflectionTestUtils.setField(deliveredOrder.getDelivery(), "deliveryStatus", DeliveryStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> Payment.requestPayment(preparingOrder))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("이미 결제 완료된 주문입니다. DeliveryStatus: " + preparingOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> Payment.requestPayment(shippedOrder))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("이미 결제 완료된 주문입니다. DeliveryStatus: " + shippedOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> Payment.requestPayment(deliveredOrder))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("이미 결제 완료된 주문입니다. DeliveryStatus: " + deliveredOrder.getDelivery().getDeliveryStatus())
            );
        }

        @Test
        void request_duplicateRequest() {
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

        @Test
        void complete_paymentStatusIsCanceled() {
            //given
            Order canceledOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Payment.requestPayment(canceledOrder);

            ReflectionTestUtils.setField(canceledOrder.getPayment(), "paymentStatus", PaymentStatus.CANCELED);

            //when
            assertThatThrownBy(() -> canceledOrder.getPayment().complete(LocalDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("취소된 주문은 결제할 수 없습니다.");
        }

        @Test
        void complete_orderStatusIsCanceled() {
            //given
            Order canceledOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Payment.requestPayment(canceledOrder);

            ReflectionTestUtils.setField(canceledOrder, "orderStatus", OrderStatus.CANCELED);

            //when
            assertThatThrownBy(() -> canceledOrder.getPayment().complete(LocalDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("취소된 주문은 결제할 수 없습니다. OrderStatus: " + canceledOrder.getOrderStatus());
        }

        @Test
        void complete_orderStatusIsPaidOrDelivered() {
            //given
            Order paidOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Payment.requestPayment(paidOrder);
            Payment.requestPayment(deliveredOrder);

            ReflectionTestUtils.setField(paidOrder, "orderStatus", OrderStatus.PAID);
            ReflectionTestUtils.setField(deliveredOrder, "orderStatus", OrderStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> paidOrder.getPayment().complete(LocalDateTime.now()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("이미 결제 완료된 주문입니다. OrderStatus: " + paidOrder.getOrderStatus()),
                    () -> assertThatThrownBy(() -> deliveredOrder.getPayment().complete(LocalDateTime.now()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("이미 결제 완료된 주문입니다. OrderStatus: " + deliveredOrder.getOrderStatus())
            );
        }

        @Test
        void complete_deliveryStatusIsCanceled() {
            //given
            Order canceledOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Payment.requestPayment(canceledOrder);

            ReflectionTestUtils.setField(canceledOrder.getDelivery(), "deliveryStatus", DeliveryStatus.CANCELED);

            //when
            assertThatThrownBy(() -> canceledOrder.getPayment().complete(LocalDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("취소된 주문은 결제할 수 없습니다. DeliveryStatus: " + canceledOrder.getDelivery().getDeliveryStatus());
        }

        @Test
        void complete_deliveryStatusIsNotWaiting() {
            //given
            Order preparingOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order shippedOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Payment.requestPayment(preparingOrder);
            Payment.requestPayment(shippedOrder);
            Payment.requestPayment(deliveredOrder);

            ReflectionTestUtils.setField(preparingOrder.getDelivery(), "deliveryStatus", DeliveryStatus.PREPARING);
            ReflectionTestUtils.setField(shippedOrder.getDelivery(), "deliveryStatus", DeliveryStatus.SHIPPED);
            ReflectionTestUtils.setField(deliveredOrder.getDelivery(), "deliveryStatus", DeliveryStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> preparingOrder.getPayment().complete(LocalDateTime.now()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("이미 결제 완료된 주문입니다. DeliveryStatus: " + preparingOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> shippedOrder.getPayment().complete(LocalDateTime.now()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("이미 결제 완료된 주문입니다. DeliveryStatus: " + shippedOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> deliveredOrder.getPayment().complete(LocalDateTime.now()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("이미 결제 완료된 주문입니다. DeliveryStatus: " + deliveredOrder.getDelivery().getDeliveryStatus())
            );
        }

        @Test
        void complete_duplicateComplete() {
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