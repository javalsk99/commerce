package lsk.commerce.domain;

import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OrderTest {

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
        void create() {
            //when
            Order createdOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));

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

        @Test
        void clear() {
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
        void clear_idempotency() {
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

        @Test
        void getOrderProductsAsMap() {
            //when
            Map<String, Integer> orderProductsAsMap = order.getOrderProductsAsMap();

            //then
            assertThat(orderProductsAsMap)
                    .hasSize(2)
                    .containsOnly(entry("BANG BANG", 5), entry("자바 ORM 표준 JPA 프로그래밍", 3));
        }

        @Test
        void update() {
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

        @Test
        void cancel_withoutPayment() {
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
        void cancel_withPayment() {
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
        void cancel_idempotency() {
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

        @Test
        void completePaid() {
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
        void completePaid_idempotency() {
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

        @Test
        void validateDeletable_withoutPayment() {
            //given
            order.cancel();

            //when
            assertDoesNotThrow(() -> order.validateDeletable());
        }

        @Test
        void validateDeletable_withPayment() {
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
        void create_addressNull() {
            //given
            Member nullAddressMember = new Member();
            Delivery nullAddressDelivery = new Delivery(nullAddressMember);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> Order.createOrder(nullAddressMember, delivery1, List.of(orderProduct1, orderProduct2)))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("배송될 주소가 없습니다."),
                    () -> assertThatThrownBy(() -> Order.createOrder(member, nullAddressDelivery, List.of(orderProduct1, orderProduct2)))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("배송될 주소가 없습니다.")
            );
        }

        @Test
        void clear_orderStatusIsNotCreated() {
            //given
            Order canceledOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order paidOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(canceledOrder, "orderStatus", OrderStatus.CANCELED);
            ReflectionTestUtils.setField(paidOrder, "orderStatus", OrderStatus.PAID);
            ReflectionTestUtils.setField(deliveredOrder, "orderStatus", OrderStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> canceledOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("주문 생성 상태가 아니어서 주문 상품을 비울 수 없습니다. OrderStatus: " + canceledOrder.getOrderStatus()),
                    () -> assertThatThrownBy(() -> paidOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("주문 생성 상태가 아니어서 주문 상품을 비울 수 없습니다. OrderStatus: " + paidOrder.getOrderStatus()),
                    () -> assertThatThrownBy(() -> deliveredOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("주문 생성 상태가 아니어서 주문 상품을 비울 수 없습니다. OrderStatus: " + deliveredOrder.getOrderStatus())
            );
        }

        @Test
        void clear_paymentStatusIsNotPending() {
            //given
            Order canceledOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order failedOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order completedOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));

            Payment.requestPayment(canceledOrder);
            Payment.requestPayment(failedOrder);
            Payment.requestPayment(completedOrder);

            ReflectionTestUtils.setField(canceledOrder.getPayment(), "paymentStatus", PaymentStatus.CANCELED);
            ReflectionTestUtils.setField(failedOrder.getPayment(), "paymentStatus", PaymentStatus.FAILED);
            ReflectionTestUtils.setField(completedOrder.getPayment(), "paymentStatus", PaymentStatus.COMPLETED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> canceledOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 대기 상태가 아니어서 주문 상품을 비울 수 없습니다. PaymentStatus: " + canceledOrder.getPayment().getPaymentStatus()),
                    () -> assertThatThrownBy(() -> failedOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 대기 상태가 아니어서 주문 상품을 비울 수 없습니다. PaymentStatus: " + failedOrder.getPayment().getPaymentStatus()),
                    () -> assertThatThrownBy(() -> completedOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 대기 상태가 아니어서 주문 상품을 비울 수 없습니다. PaymentStatus: " + completedOrder.getPayment().getPaymentStatus())
            );
        }

        @Test
        void clear_deliveryStatusIsNotWaiting() {
            //given
            Order canceledOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order preparingOrder = Order.createOrder(member, delivery2, List.of(orderProduct1, orderProduct2));
            Order shippedOrder = Order.createOrder(member, delivery3, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery4, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(canceledOrder.getDelivery(), "deliveryStatus", DeliveryStatus.CANCELED);
            ReflectionTestUtils.setField(preparingOrder.getDelivery(), "deliveryStatus", DeliveryStatus.PREPARING);
            ReflectionTestUtils.setField(shippedOrder.getDelivery(), "deliveryStatus", DeliveryStatus.SHIPPED);
            ReflectionTestUtils.setField(deliveredOrder.getDelivery(), "deliveryStatus", DeliveryStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> canceledOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니어서 주문 상품을 비울 수 없습니다. DeliveryStatus: " + canceledOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> preparingOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니어서 주문 상품을 비울 수 없습니다. DeliveryStatus: " + preparingOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> shippedOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니어서 주문 상품을 비울 수 없습니다. DeliveryStatus: " + shippedOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> deliveredOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니어서 주문 상품을 비울 수 없습니다. DeliveryStatus: " + deliveredOrder.getDelivery().getDeliveryStatus())
            );
        }

        @Test
        void cancel_orderStatusIsPaid() {
            //given
            Order paidOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(paidOrder, "orderStatus", OrderStatus.PAID);
            ReflectionTestUtils.setField(deliveredOrder, "orderStatus", OrderStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> paidOrder.cancel())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료된 주문이어서 취소할 수 없습니다. OrderStatus: " + paidOrder.getOrderStatus()),
                    () -> assertThatThrownBy(() -> deliveredOrder.cancel())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료된 주문이어서 취소할 수 없습니다. OrderStatus: " + deliveredOrder.getOrderStatus())
            );
        }

        @Test
        void cancel_paymentStatusIsCompleted() {
            //given
            Payment.requestPayment(order);

            ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", PaymentStatus.COMPLETED);

            //when
            assertThatThrownBy(() -> order.cancel())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("결제 완료돼서 취소할 수 없습니다.");
        }

        @Test
        void cancel_deliveryStatusIsNotWaiting() {
            //given
            Order preparingOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order shippedOrder = Order.createOrder(member, delivery2, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery3, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(preparingOrder.getDelivery(), "deliveryStatus", DeliveryStatus.PREPARING);
            ReflectionTestUtils.setField(shippedOrder.getDelivery(), "deliveryStatus", DeliveryStatus.SHIPPED);
            ReflectionTestUtils.setField(deliveredOrder.getDelivery(), "deliveryStatus", DeliveryStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> preparingOrder.cancel())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니여서 취소할 수 없습니다. DeliveryStatus: " + preparingOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> shippedOrder.cancel())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니여서 취소할 수 없습니다. DeliveryStatus: " + shippedOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> deliveredOrder.cancel())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니여서 취소할 수 없습니다. DeliveryStatus: " + deliveredOrder.getDelivery().getDeliveryStatus())
            );
        }

        @Test
        void completePaid_paymentStatusIsNotCompleted() {
            //given
            Order pendingOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order failedOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order canceledOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));

            Payment.requestPayment(pendingOrder);
            Payment.requestPayment(failedOrder);
            Payment.requestPayment(canceledOrder);

            ReflectionTestUtils.setField(pendingOrder.getPayment(), "paymentStatus", PaymentStatus.PENDING);
            ReflectionTestUtils.setField(failedOrder.getPayment(), "paymentStatus", PaymentStatus.FAILED);
            ReflectionTestUtils.setField(canceledOrder.getPayment(), "paymentStatus", PaymentStatus.CANCELED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> pendingOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제가 완료되지 않았습니다. PaymentStatus: " + pendingOrder.getPayment().getPaymentStatus()),
                    () -> assertThatThrownBy(() -> failedOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제가 완료되지 않았습니다. PaymentStatus: " + failedOrder.getPayment().getPaymentStatus()),
                    () -> assertThatThrownBy(() -> canceledOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제가 완료되지 않았습니다. PaymentStatus: " + canceledOrder.getPayment().getPaymentStatus())
            );
        }

        @Test
        void completePaid_orderStatusIsNotCreated() {
            //given
            Order canceldOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));

            Payment.requestPayment(canceldOrder);
            Payment.requestPayment(deliveredOrder);

            ReflectionTestUtils.setField(canceldOrder, "orderStatus", OrderStatus.CANCELED);
            ReflectionTestUtils.setField(deliveredOrder, "orderStatus", OrderStatus.DELIVERED);
            ReflectionTestUtils.setField(canceldOrder.getPayment(), "paymentStatus", PaymentStatus.COMPLETED);
            ReflectionTestUtils.setField(deliveredOrder.getPayment(), "paymentStatus", PaymentStatus.COMPLETED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> canceldOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료할 수 없는 상태입니다. OrderStatus: " + canceldOrder.getOrderStatus()),
                    () -> assertThatThrownBy(() -> deliveredOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료할 수 없는 상태입니다. OrderStatus: " + deliveredOrder.getOrderStatus())
            );
        }

        @Test
        void completePaid_deliveryStatusIsNotWaiting() {
            //given
            Order canceledOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order preparingOrder = Order.createOrder(member, delivery2, List.of(orderProduct1, orderProduct2));
            Order shippedOrder = Order.createOrder(member, delivery2, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery2, List.of(orderProduct1, orderProduct2));

            Payment.requestPayment(canceledOrder);
            Payment.requestPayment(preparingOrder);
            Payment.requestPayment(shippedOrder);
            Payment.requestPayment(deliveredOrder);

            ReflectionTestUtils.setField(canceledOrder.getDelivery(), "deliveryStatus", DeliveryStatus.CANCELED);
            ReflectionTestUtils.setField(preparingOrder.getDelivery(), "deliveryStatus", DeliveryStatus.PREPARING);
            ReflectionTestUtils.setField(shippedOrder.getDelivery(), "deliveryStatus", DeliveryStatus.SHIPPED);
            ReflectionTestUtils.setField(deliveredOrder.getDelivery(), "deliveryStatus", DeliveryStatus.DELIVERED);
            ReflectionTestUtils.setField(canceledOrder.getPayment(), "paymentStatus", PaymentStatus.COMPLETED);
            ReflectionTestUtils.setField(preparingOrder.getPayment(), "paymentStatus", PaymentStatus.COMPLETED);
            ReflectionTestUtils.setField(shippedOrder.getPayment(), "paymentStatus", PaymentStatus.COMPLETED);
            ReflectionTestUtils.setField(deliveredOrder.getPayment(), "paymentStatus", PaymentStatus.COMPLETED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> canceledOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료할 수 없는 상태입니다. DeliveryStatus: " + canceledOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> preparingOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료할 수 없는 상태입니다. DeliveryStatus: " + preparingOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> shippedOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료할 수 없는 상태입니다. DeliveryStatus: " + shippedOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> deliveredOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료할 수 없는 상태입니다. DeliveryStatus: " + deliveredOrder.getDelivery().getDeliveryStatus())
            );
        }

        @Test
        void validateDeletable_orderStatusIsCreated() {
            //given
            Order createdOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(createdOrder, "orderStatus", OrderStatus.CREATED);

            //when
            assertThatThrownBy(() -> createdOrder.validateDeletable())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("주문을 취소해야 삭제할 수 있습니다.");
        }

        @Test
        void validateDeletable_orderStatusIsPaid() {
            //given
            Order paidOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Payment.requestPayment(paidOrder);

            ReflectionTestUtils.setField(paidOrder, "orderStatus", OrderStatus.PAID);

            //when
            assertThatThrownBy(() -> paidOrder.validateDeletable())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("배송이 완료돼야 삭제할 수 있습니다.");
        }

        @Test
        void validateDeletable_deliveryStatusIsWaiting() {
            //given
            Order waitingOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(waitingOrder, "orderStatus", OrderStatus.CANCELED);
            ReflectionTestUtils.setField(waitingOrder.getDelivery(), "deliveryStatus", DeliveryStatus.WAITING);

            //when
            assertThatThrownBy(() -> waitingOrder.validateDeletable())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("주문을 취소해야 삭제할 수 있습니다. DeliveryStatus: " + waitingOrder.getDelivery().getDeliveryStatus());
        }

        @Test
        void validateDeletable_deliveryStatusIsPreparingOrShipped() {
            //given
            Order preparingOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order shippedOrder = Order.createOrder(member, delivery2, List.of(orderProduct1, orderProduct2));
            Payment.requestPayment(preparingOrder);
            Payment.requestPayment(shippedOrder);

            ReflectionTestUtils.setField(preparingOrder, "orderStatus", OrderStatus.DELIVERED);
            ReflectionTestUtils.setField(shippedOrder, "orderStatus", OrderStatus.DELIVERED);
            ReflectionTestUtils.setField(preparingOrder.getDelivery(), "deliveryStatus", DeliveryStatus.PREPARING);
            ReflectionTestUtils.setField(shippedOrder.getDelivery(), "deliveryStatus", DeliveryStatus.SHIPPED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> preparingOrder.validateDeletable())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송이 완료돼야 삭제할 수 있습니다. DeliveryStatus: " + preparingOrder.getDelivery().getDeliveryStatus()),
                    () -> assertThatThrownBy(() -> shippedOrder.validateDeletable())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송이 완료돼야 삭제할 수 있습니다. DeliveryStatus: " + shippedOrder.getDelivery().getDeliveryStatus())
            );
        }

        @Test
        void validateDeletable_paymentStatusIsPendingOrFailed() {
            //given
            Order pendingOrder = Order.createOrder(member, delivery1, List.of(orderProduct1, orderProduct2));
            Order failedOrder = Order.createOrder(member, delivery2, List.of(orderProduct1, orderProduct2));
            Payment.requestPayment(pendingOrder);
            Payment.requestPayment(failedOrder);

            ReflectionTestUtils.setField(pendingOrder, "orderStatus", OrderStatus.CANCELED);
            ReflectionTestUtils.setField(failedOrder, "orderStatus", OrderStatus.CANCELED);
            ReflectionTestUtils.setField(pendingOrder.getDelivery(), "deliveryStatus", DeliveryStatus.CANCELED);
            ReflectionTestUtils.setField(failedOrder.getDelivery(), "deliveryStatus", DeliveryStatus.CANCELED);
            ReflectionTestUtils.setField(pendingOrder.getPayment(), "paymentStatus", PaymentStatus.PENDING);
            ReflectionTestUtils.setField(failedOrder.getPayment(), "paymentStatus", PaymentStatus.FAILED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> pendingOrder.validateDeletable())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("주문을 취소해야 삭제할 수 있습니다. PaymentStatus: " + pendingOrder.getPayment().getPaymentStatus()),
                    () -> assertThatThrownBy(() -> failedOrder.validateDeletable())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("주문을 취소해야 삭제할 수 있습니다. PaymentStatus: " + failedOrder.getPayment().getPaymentStatus())
            );
        }
    }
}