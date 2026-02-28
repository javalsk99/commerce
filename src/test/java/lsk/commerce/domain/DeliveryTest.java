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

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

class DeliveryTest {

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

        Payment.requestPayment(order);

        order.getPayment().complete(LocalDateTime.now());
        order.completePaid();
    }

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                Delivery correctAddressDelivery = new Delivery(member);

                //then
                assertAll(
                        () -> assertThat(correctAddressDelivery.getAddress())
                                .extracting("city", "street", "zipcode")
                                .containsExactly("Seoul", "Gangnam", "01234"),
                        () -> assertThat(correctAddressDelivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.WAITING)
                );
            }
        }

        @Nested
        class FailureCase {

            @Test
            void memberAddressIsNull() {
                //given
                Member nullAddressMember = new Member();

                //when
                assertThatThrownBy(() -> new Delivery(nullAddressMember))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("회원의 주소 정보가 없습니다.");
            }

            @ParameterizedTest
            @MethodSource("addressProvider")
            void wrongMemberAddress(String city, String street, String zipcode, String message) {
                //given
                Member wrongAddressMember = Member.builder().city(city).street(street).zipcode(zipcode).build();

                //when
                assertThatThrownBy(() -> new Delivery(wrongAddressMember))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage(message);
            }

            static Stream<Arguments> addressProvider() {
                return Stream.of(
                        argumentSet("city null", null, "Gangnam", "01234", "회원의 주소 정보가 잘못됐습니다. address.city = null, address.street = Gangnam, address.zipcode = 01234"),
                        argumentSet("street null", "Seoul", null, "01234", "회원의 주소 정보가 잘못됐습니다. address.city = Seoul, address.street = null, address.zipcode = 01234"),
                        argumentSet("zipcode null", "Seoul", "Gangnam", null, "회원의 주소 정보가 잘못됐습니다. address.city = Seoul, address.street = Gangnam, address.zipcode = null")
                );
            }
        }
    }

    @Nested
    class StartDelivery {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                delivery.startDelivery();

                //then
                assertAll(
                        () -> assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED),
                        () -> assertThat(delivery.getShippedDate()).isNotNull(),
                        () -> assertThat(delivery.getDeliveredDate()).isNull()
                );
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("lsk.commerce.domain.DeliveryTest#orderStatusProvider")
            void orderStatusIsNotPaid(OrderStatus orderStatus, String message) {
                //given
                Order notPaidOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

                ReflectionTestUtils.setField(notPaidOrder, "orderStatus", orderStatus);

                //when
                assertThatThrownBy(() -> delivery.startDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            @ParameterizedTest
            @MethodSource("lsk.commerce.domain.DeliveryTest#paymentStatusProvider")
            void paymentStatusIsNotCompleted(PaymentStatus paymentStatus, String message) {
                //given
                ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", paymentStatus);

                //when
                assertThatThrownBy(() -> delivery.startDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            @ParameterizedTest
            @MethodSource("deliveryStatusProvider")
            void deliveryStatusIsNotPreparing(DeliveryStatus deliveryStatus, String message) {
                //given
                ReflectionTestUtils.setField(delivery, "deliveryStatus", deliveryStatus);

                //when
                assertThatThrownBy(() -> delivery.startDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            static Stream<Arguments> deliveryStatusProvider() {
                return Stream.of(
                        argumentSet("DeliveryStatus: WAITING", DeliveryStatus.WAITING, "결제 완료된 주문이 아닙니다. DeliveryStatus: WAITING"),
                        argumentSet("DeliveryStatus: CANCELED", DeliveryStatus.CANCELED, "결제 완료된 주문이 아닙니다. DeliveryStatus: CANCELED"),
                        argumentSet("DeliveryStatus: SHIPPED", DeliveryStatus.SHIPPED, "이미 발송된 주문입니다. DeliveryStatus: SHIPPED"),
                        argumentSet("DeliveryStatus: DELIVERED", DeliveryStatus.DELIVERED, "이미 발송된 주문입니다. DeliveryStatus: DELIVERED")
                );
            }
        }
    }

    @Nested
    class CompleteDelivery {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                delivery.startDelivery();

                //when
                delivery.completeDelivery();

                //then
                assertAll(
                        () -> assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED),
                        () -> assertThat(delivery.getShippedDate()).isNotNull(),
                        () -> assertThat(delivery.getDeliveredDate()).isNotNull()
                );
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("lsk.commerce.domain.DeliveryTest#orderStatusProvider")
            void orderStatusIsNotPaid(OrderStatus orderStatus, String message) {
                //given
                Order notPaidOrder = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

                ReflectionTestUtils.setField(notPaidOrder, "orderStatus", orderStatus);

                //when
                assertThatThrownBy(() -> delivery.completeDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            @ParameterizedTest
            @MethodSource("lsk.commerce.domain.DeliveryTest#paymentStatusProvider")
            void paymentStatusIsNotCompleted(PaymentStatus paymentStatus, String message) {
                //given
                ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", paymentStatus);

                //when
                assertThatThrownBy(() -> delivery.completeDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            @ParameterizedTest
            @MethodSource("deliveryStatusProvider")
            void deliveryStatusIsNotShipped(DeliveryStatus deliveryStatus, String message) {
                //given
                ReflectionTestUtils.setField(delivery, "deliveryStatus", deliveryStatus);

                //when
                assertThatThrownBy(() -> delivery.completeDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            static Stream<Arguments> deliveryStatusProvider() {
                return Stream.of(
                        argumentSet("DeliveryStatus: WAITING", DeliveryStatus.WAITING, "발송된 주문이 아닙니다. DeliveryStatus: WAITING"),
                        argumentSet("DeliveryStatus: CANCELED", DeliveryStatus.CANCELED, "발송된 주문이 아닙니다. DeliveryStatus: CANCELED"),
                        argumentSet("DeliveryStatus: PREPARING", DeliveryStatus.PREPARING, "발송된 주문이 아닙니다. DeliveryStatus: PREPARING"),
                        argumentSet("DeliveryStatus: DELIVERED", DeliveryStatus.DELIVERED, "이미 배송 완료된 주문입니다. DeliveryStatus: DELIVERED")
                );
            }
        }
    }

    static Stream<Arguments> orderStatusProvider() {
        return Stream.of(
                argumentSet("OrderStatus: CREATED", OrderStatus.CREATED, "결제 완료된 주문이 아닙니다. OrderStatus: CREATED"),
                argumentSet("OrderStatus: CANCELED", OrderStatus.CANCELED, "결제 완료된 주문이 아닙니다. OrderStatus: CANCELED"),
                argumentSet("OrderStatus: DELIVERED", OrderStatus.DELIVERED, "이미 배송 완료된 주문입니다. OrderStatus: DELIVERED")
        );
    }

    static Stream<Arguments> paymentStatusProvider() {
        return Stream.of(
                argumentSet("PaymentStatus: PENDING", PaymentStatus.PENDING, "결제 완료된 주문이 아닙니다. PaymentStatus: PENDING"),
                argumentSet("PaymentStatus: CANCELED", PaymentStatus.CANCELED, "결제 완료된 주문이 아닙니다. PaymentStatus: CANCELED"),
                argumentSet("PaymentStatus: FAILED", PaymentStatus.FAILED, "결제 완료된 주문이 아닙니다. PaymentStatus: FAILED")
        );
    }
}