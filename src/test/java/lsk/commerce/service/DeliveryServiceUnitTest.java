package lsk.commerce.service;

import lsk.commerce.domain.Category;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.MemberRequest;
import lsk.commerce.event.DeliveryStartedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceUnitTest {

    @Mock
    OrderService orderService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    DeliveryService deliveryService;

    Member member;
    Delivery delivery;
    Album album;
    Book book;
    Movie movie;
    OrderProduct orderProduct1;
    OrderProduct orderProduct2;
    OrderProduct orderProduct3;
    Order order;
    String wrongOrderNumber = "lllIIllIO00O";
    String wrongPaymentId = "dfoijxjfd342987jdfk";

    @BeforeEach
    void beforeEach() {
        member = Member.builder().city("Seoul").street("Gangnam").zipcode("01234").build();
        delivery = new Delivery(member);

        album = Album.builder().name("BANG BANG").price(15000).stockQuantity(10).build();
        book = Book.builder().name("자바 ORM 표준 JPA 프로그래밍").price(15000).stockQuantity(7).build();
        movie = Movie.builder().name("범죄도시").price(15000).stockQuantity(5).build();

        orderProduct1 = OrderProduct.createOrderProduct(album, 5);
        orderProduct2 = OrderProduct.createOrderProduct(book, 3);
        orderProduct3 = OrderProduct.createOrderProduct(movie, 2);

        order = Order.createOrder(member, delivery, List.of(orderProduct3));
        Payment.requestPayment(order);

        order.getPayment().complete(LocalDateTime.now());
        order.completePaid();
    }

    @Nested
    class StartDelivery {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                given(orderService.findOrderWithDelivery(anyString())).willReturn(order);

                //when
                deliveryService.startDelivery(order.getOrderNumber());

                //then
                assertAll(
                        () -> then(orderService).should().findOrderWithDelivery(anyString()),
                        () -> then(eventPublisher).should().publishEvent(any(DeliveryStartedEvent.class))
                );
                assertAll(
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED)
                );
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderNotFound() {
                //given
                given(orderService.findOrderWithDelivery(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 주문입니다."));

                //when
                assertThatThrownBy(() -> deliveryService.startDelivery(wrongOrderNumber))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 주문입니다.");

                //then
                assertAll(
                        () -> then(orderService).should().findOrderWithDelivery(anyString()),
                        () -> then(eventPublisher).should(never()).publishEvent(any())
                );
            }

            @Test
            void failedEventPublisher() {
                //given
                given(orderService.findOrderWithDelivery(anyString())).willReturn(order);
                willThrow(new RuntimeException("Event Publish Failed")).given(eventPublisher).publishEvent(any(DeliveryStartedEvent.class));

                //when
                assertThatThrownBy(() -> deliveryService.startDelivery(order.getOrderNumber()))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Event Publish Failed");

                //then
                assertAll(
                        () -> then(orderService).should().findOrderWithDelivery(anyString()),
                        () -> then(eventPublisher).should().publishEvent(any(DeliveryStartedEvent.class))
                );
            }

            @Test
            void alreadyShipped() {
                //given
                given(orderService.findOrderWithDelivery(anyString())).willReturn(order);

                //when 첫 번째 호출
                deliveryService.startDelivery(order.getOrderNumber());

                //then
                assertAll(
                        () -> then(orderService).should().findOrderWithDelivery(anyString()),
                        () -> then(eventPublisher).should().publishEvent(any(DeliveryStartedEvent.class))
                );
                assertAll(
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED)
                );

                //when 두 번째 호출
                assertThatThrownBy(() -> deliveryService.startDelivery(order.getOrderNumber()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 발송된 주문입니다. DeliveryStatus: SHIPPED");

                //then
                assertAll(
                        () -> then(orderService).should(times(2)).findOrderWithDelivery(anyString()),
                        () -> then(eventPublisher).should().publishEvent(any(Object.class))
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
                ReflectionTestUtils.setField(delivery, "deliveryStatus", DeliveryStatus.SHIPPED);

                given(orderService.findOrderWithDelivery(anyString())).willReturn(order);

                //when
                deliveryService.completeDelivery(order.getOrderNumber());

                //then
                then(orderService).should().findOrderWithDelivery(anyString());
                assertAll(
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED)
                );
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderNotFound() {
                //given
                given(orderService.findOrderWithDelivery(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 주문입니다."));

                //when
                assertThatThrownBy(() -> deliveryService.completeDelivery(wrongOrderNumber))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 주문입니다.");

                //then
                then(orderService).should().findOrderWithDelivery(anyString());
            }

            @Test
            void alreadyDelivered() {
                //given
                ReflectionTestUtils.setField(delivery, "deliveryStatus", DeliveryStatus.SHIPPED);

                given(orderService.findOrderWithDelivery(anyString())).willReturn(order);

                //when
                deliveryService.completeDelivery(order.getOrderNumber());

                //then
                then(orderService).should().findOrderWithDelivery(anyString());
                assertAll(
                        () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED),
                        () -> assertThat(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED)
                );

                //when
                assertThatThrownBy(() -> deliveryService.completeDelivery(order.getOrderNumber()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 배송 완료된 주문입니다. OrderStatus: DELIVERED");
            }
        }
    }
/*

    @Test
    void failed_completeDelivery_alreadyDelivered() {
        //given
//        paymentService.completePayment(paymentId);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        deliveryService.startDelivery(orderNumber);
        deliveryService.completeDelivery(orderNumber);

        //when
        assertThatThrownBy(() -> deliveryService.completeDelivery(orderNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 배송 완료된 주문입니다.");
    }
*/
}