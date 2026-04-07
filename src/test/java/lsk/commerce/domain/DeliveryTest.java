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

import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class DeliveryTest {

    Member member;

    @BeforeEach
    void beforeEach() {
        member = Member.builder()
                .zipcode("01234")
                .baseAddress("서울시 강남구")
                .detailAddress("101동 101호")
                .build();
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
                thenSoftly(softly -> {
                    softly.then(correctAddressDelivery.getAddress())
                            .extracting("zipcode", "baseAddress", "detailAddress")
                            .containsExactly("01234", "서울시 강남구", "101동 101호");
                    softly.then(correctAddressDelivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.WAITING);
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void memberAddressIsNull() {
                //given
                Member nullAddressMember = new Member();

                //when & then
                thenThrownBy(() -> new Delivery(nullAddressMember))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("회원의 주소 정보가 없습니다");
            }

            @ParameterizedTest
            @MethodSource("addressProvider")
            void wrongMemberAddress(String zipcode, String baseAddress, String detailAddress, String message) {
                //given
                Member wrongAddressMember = Member.builder()
                        .zipcode(zipcode)
                        .baseAddress(baseAddress)
                        .detailAddress(detailAddress)
                        .build();

                //when & then
                thenThrownBy(() -> new Delivery(wrongAddressMember))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage(message);
            }

            static Stream<Arguments> addressProvider() {
                return Stream.of(
                        argumentSet("zipcode null", null, "서울시 강남구", "101동 101호", "회원의 주소 정보가 잘못됐습니다. address.zipcode = " + null + ", address.baseAddress = " + "서울시 강남구" + ", address.detailAddress = " + "101동 101호"),
                        argumentSet("baseAddress null", "01234", null, "101동 101호", "회원의 주소 정보가 잘못됐습니다. address.zipcode = " + "01234" + ", address.baseAddress = " + null + ", address.detailAddress = " + "101동 101호"),
                        argumentSet("detailAddress null", "01234", "서울시 강남구", null, "회원의 주소 정보가 잘못됐습니다. address.zipcode = " + "01234" + ", address.baseAddress = " + "서울시 강남구" + ", address.detailAddress = " + null)
                );
            }
        }
    }

    abstract class Setup {

        Delivery delivery;
        Album album;
        Book book;
        Movie movie;
        OrderProduct orderProduct1;
        OrderProduct orderProduct2;
        Order order;

        @BeforeEach
        void beforeEach() {
            delivery = new Delivery(member);

            album = Album.builder()
                    .name("BANG BANG")
                    .price(15000)
                    .stockQuantity(10)
                    .build();
            book = Book.builder()
                    .name("자바 ORM 표준 JPA 프로그래밍")
                    .price(15000)
                    .stockQuantity(7)
                    .build();
            movie = Movie.builder()
                    .name("범죄도시")
                    .price(15000)
                    .stockQuantity(5)
                    .build();

            orderProduct1 = OrderProduct.createOrderProduct(album, 5);
            orderProduct2 = OrderProduct.createOrderProduct(book, 3);

            order = Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));

            Payment.requestPayment(order);

            order.getPayment().complete(LocalDateTime.now());
            order.completePaid();
        }
    }

    @Nested
    class StartDelivery extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //when
                delivery.startDelivery();

                //then
                thenSoftly(softly -> {
                    softly.then(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED);
                    softly.then(delivery.getShippedDate()).isNotNull();
                    softly.then(delivery.getDeliveredDate()).isNull();
                });
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

                //when & then
                thenThrownBy(() -> delivery.startDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            @ParameterizedTest
            @MethodSource("lsk.commerce.domain.DeliveryTest#paymentStatusProvider")
            void paymentStatusIsNotCompleted(PaymentStatus paymentStatus, String message) {
                //given
                ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", paymentStatus);

                //when & then
                thenThrownBy(() -> delivery.startDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            @ParameterizedTest
            @MethodSource("deliveryStatusProvider")
            void deliveryStatusIsNotPreparing(DeliveryStatus deliveryStatus, String message) {
                //given
                ReflectionTestUtils.setField(delivery, "deliveryStatus", deliveryStatus);

                //when & then
                thenThrownBy(() -> delivery.startDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            static Stream<Arguments> deliveryStatusProvider() {
                return Stream.of(
                        argumentSet("DeliveryStatus: WAITING", DeliveryStatus.WAITING, "결제 완료된 주문이 아닙니다. DeliveryStatus: " + DeliveryStatus.WAITING),
                        argumentSet("DeliveryStatus: CANCELED", DeliveryStatus.CANCELED, "결제 완료된 주문이 아닙니다. DeliveryStatus: " + DeliveryStatus.CANCELED),
                        argumentSet("DeliveryStatus: SHIPPED", DeliveryStatus.SHIPPED, "이미 발송된 주문입니다. DeliveryStatus: " + DeliveryStatus.SHIPPED),
                        argumentSet("DeliveryStatus: DELIVERED", DeliveryStatus.DELIVERED, "이미 발송된 주문입니다. DeliveryStatus: " + DeliveryStatus.DELIVERED)
                );
            }
        }
    }

    @Nested
    class CompleteDelivery extends Setup {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                delivery.startDelivery();

                //when
                delivery.completeDelivery();

                //then
                thenSoftly(softly -> {
                    softly.then(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
                    softly.then(delivery.getShippedDate()).isNotNull();
                    softly.then(delivery.getDeliveredDate()).isNotNull();
                });
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

                //when & then
                thenThrownBy(() -> delivery.completeDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            @ParameterizedTest
            @MethodSource("lsk.commerce.domain.DeliveryTest#paymentStatusProvider")
            void paymentStatusIsNotCompleted(PaymentStatus paymentStatus, String message) {
                //given
                ReflectionTestUtils.setField(order.getPayment(), "paymentStatus", paymentStatus);

                //when & then
                thenThrownBy(() -> delivery.completeDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            @ParameterizedTest
            @MethodSource("deliveryStatusProvider")
            void deliveryStatusIsNotShipped(DeliveryStatus deliveryStatus, String message) {
                //given
                ReflectionTestUtils.setField(delivery, "deliveryStatus", deliveryStatus);

                //when & then
                thenThrownBy(() -> delivery.completeDelivery())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }

            static Stream<Arguments> deliveryStatusProvider() {
                return Stream.of(
                        argumentSet("DeliveryStatus: WAITING", DeliveryStatus.WAITING, "발송된 주문이 아닙니다. DeliveryStatus: " + DeliveryStatus.WAITING),
                        argumentSet("DeliveryStatus: CANCELED", DeliveryStatus.CANCELED, "발송된 주문이 아닙니다. DeliveryStatus: " + DeliveryStatus.CANCELED),
                        argumentSet("DeliveryStatus: PREPARING", DeliveryStatus.PREPARING, "발송된 주문이 아닙니다. DeliveryStatus: " + DeliveryStatus.PREPARING),
                        argumentSet("DeliveryStatus: DELIVERED", DeliveryStatus.DELIVERED, "이미 배송 완료된 주문입니다. DeliveryStatus: " + DeliveryStatus.DELIVERED)
                );
            }
        }
    }

    static Stream<Arguments> orderStatusProvider() {
        return Stream.of(
                argumentSet("OrderStatus: CREATED", OrderStatus.CREATED, "결제 완료된 주문이 아닙니다. OrderStatus: " + OrderStatus.CREATED),
                argumentSet("OrderStatus: CANCELED", OrderStatus.CANCELED, "결제 완료된 주문이 아닙니다. OrderStatus: " + OrderStatus.CANCELED),
                argumentSet("OrderStatus: DELIVERED", OrderStatus.DELIVERED, "이미 배송 완료된 주문입니다. OrderStatus: " + OrderStatus.DELIVERED)
        );
    }

    static Stream<Arguments> paymentStatusProvider() {
        return Stream.of(
                argumentSet("PaymentStatus: PENDING", PaymentStatus.PENDING, "결제 완료된 주문이 아닙니다. PaymentStatus: " + PaymentStatus.PENDING),
                argumentSet("PaymentStatus: CANCELED", PaymentStatus.CANCELED, "결제 완료된 주문이 아닙니다. PaymentStatus: " + PaymentStatus.CANCELED),
                argumentSet("PaymentStatus: FAILED", PaymentStatus.FAILED, "결제 완료된 주문이 아닙니다. PaymentStatus: " + PaymentStatus.FAILED)
        );
    }
}