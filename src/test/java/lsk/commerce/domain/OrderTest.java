package lsk.commerce.domain;

import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;
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
        album = Album.builder().price(15000).stockQuantity(10).build();
        book = Book.builder().price(15000).stockQuantity(7).build();
        movie = Movie.builder().price(15000).stockQuantity(5).build();
        orderProduct1 = OrderProduct.createOrderProduct(album, 5);
        orderProduct2 = OrderProduct.createOrderProduct(book, 3);
        order = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
    }

    @Nested
    class SuccessCase {

        @Test
        void create() {
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
        void cancel_paymentNull() {
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
        void cancel_paymentNotNull() {
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
    }

    @Nested
    class FailureCase {

        @Test
        void create_memberNull() {
            //when
            assertThatThrownBy(() -> Order.createOrder(null, delivery, List.of(orderProduct1, orderProduct2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문할 회원이 없습니다.");
        }

        @Test
        void create_deliveryNull() {
            //when
            assertThatThrownBy(() -> Order.createOrder(member, null, List.of(orderProduct1, orderProduct2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("배송 정보가 없습니다.");
        }

        @Test
        void create_addressNull() {
            //given
            Member nullAddressMember = new Member();
            Delivery nullAddressDelivery = new Delivery(nullAddressMember);

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

        @Test
        void create_notExistsOrderProducts() {
            //when
            assertAll(
                    () -> assertThatThrownBy(() -> Order.createOrder(member, delivery, null))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("주문 상품이 없습니다."),
                    () -> assertThatThrownBy(() -> Order.createOrder(member, delivery, Collections.emptyList()))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("주문 상품이 없습니다.")
            );
        }

        @Test
        void clear_byOrderStatus() {
            //given
            Order canceledOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order paidOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(canceledOrder, "orderStatus", OrderStatus.CANCELED);
            ReflectionTestUtils.setField(paidOrder, "orderStatus", OrderStatus.PAID);
            ReflectionTestUtils.setField(deliveredOrder, "orderStatus", OrderStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> canceledOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("주문 생성 상태가 아니어서 주문 상품을 비울 수 없습니다."),
                    () -> assertThatThrownBy(() -> paidOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("주문 생성 상태가 아니어서 주문 상품을 비울 수 없습니다."),
                    () -> assertThatThrownBy(() -> deliveredOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("주문 생성 상태가 아니어서 주문 상품을 비울 수 없습니다.")
            );
        }

        @Test
        void clear_byPaymentStatus() {
            //given
            Order canceledOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order failedOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order completedOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            Payment canceledPayment = new Payment();
            Payment failedPayment = new Payment();
            Payment completedPayment = new Payment();

            ReflectionTestUtils.setField(canceledPayment, "paymentStatus", PaymentStatus.CANCELED);
            ReflectionTestUtils.setField(failedPayment, "paymentStatus", PaymentStatus.FAILED);
            ReflectionTestUtils.setField(completedPayment, "paymentStatus", PaymentStatus.COMPLETED);
            ReflectionTestUtils.setField(canceledOrder, "payment", canceledPayment);
            ReflectionTestUtils.setField(failedOrder, "payment", failedPayment);
            ReflectionTestUtils.setField(completedOrder, "payment", completedPayment);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> canceledOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 대기 상태가 아니어서 주문 상품을 비울 수 없습니다."),
                    () -> assertThatThrownBy(() -> failedOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 대기 상태가 아니어서 주문 상품을 비울 수 없습니다."),
                    () -> assertThatThrownBy(() -> completedOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 대기 상태가 아니어서 주문 상품을 비울 수 없습니다.")
            );
        }

        @Test
        void clear_byDeliveryStatus() {
            //given
            Order canceledOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order preparingOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order shippedOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(canceledOrder.getDelivery(), "deliveryStatus", DeliveryStatus.CANCELED);
            ReflectionTestUtils.setField(preparingOrder.getDelivery(), "deliveryStatus", DeliveryStatus.PREPARING);
            ReflectionTestUtils.setField(shippedOrder.getDelivery(), "deliveryStatus", DeliveryStatus.SHIPPED);
            ReflectionTestUtils.setField(deliveredOrder.getDelivery(), "deliveryStatus", DeliveryStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> canceledOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니어서 주문 상품을 비울 수 없습니다."),
                    () -> assertThatThrownBy(() -> preparingOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니어서 주문 상품을 비울 수 없습니다."),
                    () -> assertThatThrownBy(() -> shippedOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니어서 주문 상품을 비울 수 없습니다."),
                    () -> assertThatThrownBy(() -> deliveredOrder.clearOrderProduct())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니어서 주문 상품을 비울 수 없습니다.")
            );
        }

        @Test
        void update_notClearedOrderProduct() {
            //given
            Order notClearedOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(notClearedOrder, "orderProducts", null);

            OrderProduct orderProduct3 = OrderProduct.createOrderProduct(movie, 2);
            OrderProduct orderProduct4 = OrderProduct.createOrderProduct(album, 2);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> notClearedOrder.updateOrder(List.of(orderProduct3, orderProduct4)))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("주문 상품이 없습니다."),
                    () -> assertThatThrownBy(() -> order.updateOrder(List.of(orderProduct3, orderProduct4)))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("주문 상품이 비어 있지 않습니다.")

            );
        }

        @Test
        void update_notExistsOrderProducts() {
            //given
            order.clearOrderProduct();

            //when
            assertThatThrownBy(() -> order.updateOrder(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("수정할 주문 상품이 없습니다.");
        }

        @Test
        void cancel_byOrderStatus() {
            //given
            Order paidOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(paidOrder, "orderStatus", OrderStatus.PAID);
            ReflectionTestUtils.setField(deliveredOrder, "orderStatus", OrderStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> paidOrder.cancel())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료된 주문이어서 취소할 수 없습니다."),
                    () -> assertThatThrownBy(() -> deliveredOrder.cancel())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료된 주문이어서 취소할 수 없습니다.")
            );
        }

        @Test
        void cancel_byPaymentStatus() {
            //given
            Payment payment = new Payment();

            ReflectionTestUtils.setField(payment, "paymentStatus", PaymentStatus.COMPLETED);
            ReflectionTestUtils.setField(order, "payment", payment);

            //when
            assertThatThrownBy(() -> order.cancel())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("결제 완료돼서 취소할 수 없습니다.");
        }

        @Test
        void cancel_byDeliveryStatus() {
            //given
            Order waitingOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order shippedOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            ReflectionTestUtils.setField(waitingOrder.getDelivery(), "deliveryStatus", DeliveryStatus.WAITING);
            ReflectionTestUtils.setField(shippedOrder.getDelivery(), "deliveryStatus", DeliveryStatus.SHIPPED);
            ReflectionTestUtils.setField(deliveredOrder.getDelivery(), "deliveryStatus", DeliveryStatus.DELIVERED);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> waitingOrder.cancel())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니여서 취소할 수 없습니다."),
                    () -> assertThatThrownBy(() -> shippedOrder.cancel())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니여서 취소할 수 없습니다."),
                    () -> assertThatThrownBy(() -> deliveredOrder.cancel())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아니여서 취소할 수 없습니다.")
            );
        }

        @Test
        void completePaid_paymentNull() {
            //when
            assertThatThrownBy(() -> order.completePaid())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("진행 중인 결제가 없습니다.");
        }

        @Test
        void completePaid_byPaymentStatus() {
            //given
            Order pendingOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order failedOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order canceledOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            Payment pendingPayment = new Payment();
            Payment failedPayment = new Payment();
            Payment canceledPayment = new Payment();


            ReflectionTestUtils.setField(pendingPayment, "paymentStatus", PaymentStatus.PENDING);
            ReflectionTestUtils.setField(failedPayment, "paymentStatus", PaymentStatus.FAILED);
            ReflectionTestUtils.setField(canceledPayment, "paymentStatus", PaymentStatus.CANCELED);
            ReflectionTestUtils.setField(pendingOrder, "payment", pendingPayment);
            ReflectionTestUtils.setField(failedOrder, "payment", failedPayment);
            ReflectionTestUtils.setField(canceledOrder, "payment", canceledPayment);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> pendingOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제가 완료되지 않았습니다."),
                    () -> assertThatThrownBy(() -> failedOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제가 완료되지 않았습니다."),
                    () -> assertThatThrownBy(() -> canceledOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제가 완료되지 않았습니다.")
            );
        }

        @Test
        void completePaid_byOrderStatus() {
            //given
            Order canceldOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            Payment completePayment = new Payment();

            ReflectionTestUtils.setField(canceldOrder, "orderStatus", OrderStatus.CANCELED);
            ReflectionTestUtils.setField(deliveredOrder, "orderStatus", OrderStatus.DELIVERED);
            ReflectionTestUtils.setField(completePayment, "paymentStatus", PaymentStatus.COMPLETED);
            ReflectionTestUtils.setField(canceldOrder, "payment", completePayment);
            ReflectionTestUtils.setField(deliveredOrder, "payment", completePayment);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> canceldOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료 처리가 불가능한 주문입니다."),
                    () -> assertThatThrownBy(() -> deliveredOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("결제 완료 처리가 불가능한 주문입니다.")
            );
        }

        @Test
        void completePaid_byDeliveryStatus() {
            //given
            Order shippedOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
            Order deliveredOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            Payment completePayment = new Payment();

            ReflectionTestUtils.setField(shippedOrder.getDelivery(), "deliveryStatus", DeliveryStatus.SHIPPED);
            ReflectionTestUtils.setField(deliveredOrder.getDelivery(), "deliveryStatus", DeliveryStatus.DELIVERED);
            ReflectionTestUtils.setField(completePayment, "paymentStatus", PaymentStatus.COMPLETED);
            ReflectionTestUtils.setField(shippedOrder, "payment", completePayment);
            ReflectionTestUtils.setField(deliveredOrder, "payment", completePayment);

            //when
            assertAll(
                    () -> assertThatThrownBy(() -> shippedOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아닙니다."),
                    () -> assertThatThrownBy(() -> deliveredOrder.completePaid())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("배송 대기 상태가 아닙니다.")
            );
        }
    }
}