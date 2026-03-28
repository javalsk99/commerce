package lsk.commerce.service;

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
import lsk.commerce.event.DeliveryStartedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

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

    @BeforeEach
    void beforeEach() {
        member = Member.builder()
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
                .build();
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
                given(orderService.findOrderWithDeliveryPayment(anyString())).willReturn(order);

                //when
                deliveryService.startDelivery(order.getOrderNumber());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should().publishEvent(any(DeliveryStartedEvent.class)));
                });
                thenSoftly(softly -> {
                    softly.then(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
                    softly.then(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED);
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderNotFound() {
                //given
                given(orderService.findOrderWithDeliveryPayment(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 주문입니다"));

                //when & then
                thenThrownBy(() -> deliveryService.startDelivery(wrongOrderNumber))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 주문입니다");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should(never()).publishEvent(any()));
                });
            }

            @Test
            void failedEventPublisher() {
                //given
                given(orderService.findOrderWithDeliveryPayment(anyString())).willReturn(order);
                willThrow(new RuntimeException("Event Publish Failed")).given(eventPublisher).publishEvent(any(DeliveryStartedEvent.class));

                //when & then
                thenThrownBy(() -> deliveryService.startDelivery(order.getOrderNumber()))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Event Publish Failed");

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should().publishEvent(any(DeliveryStartedEvent.class)));
                });
            }

            @Test
            void alreadyShipped() {
                //given
                given(orderService.findOrderWithDeliveryPayment(anyString())).willReturn(order);

                //when 첫 번째 호출
                deliveryService.startDelivery(order.getOrderNumber());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should().publishEvent(any(DeliveryStartedEvent.class)));
                });
                thenSoftly(softly -> {
                    softly.then(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
                    softly.then(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED);
                });

                //when & then 두 번째 호출
                thenThrownBy(() -> deliveryService.startDelivery(order.getOrderNumber()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 발송된 주문입니다. DeliveryStatus: " + DeliveryStatus.SHIPPED);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should(times(2)).findOrderWithDeliveryPayment(anyString()));
                    softly.check(() -> BDDMockito.then(eventPublisher).should().publishEvent(any(Object.class)));
                });
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

                given(orderService.findOrderWithDeliveryPayment(anyString())).willReturn(order);

                //when
                deliveryService.completeDelivery(order.getOrderNumber());

                //then
                BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(anyString());
                thenSoftly(softly -> {
                    softly.then(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
                    softly.then(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void orderNotFound() {
                //given
                given(orderService.findOrderWithDeliveryPayment(anyString())).willThrow(new IllegalArgumentException("존재하지 않는 주문입니다"));

                //when & then
                thenThrownBy(() -> deliveryService.completeDelivery(wrongOrderNumber))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("존재하지 않는 주문입니다");

                //then
                BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(anyString());
            }

            @Test
            void alreadyDelivered() {
                //given
                ReflectionTestUtils.setField(delivery, "deliveryStatus", DeliveryStatus.SHIPPED);

                given(orderService.findOrderWithDeliveryPayment(anyString())).willReturn(order);

                //when 첫 번째 호출
                deliveryService.completeDelivery(order.getOrderNumber());

                //then
                BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(anyString());
                thenSoftly(softly -> {
                    softly.then(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
                    softly.then(order.getDelivery().getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
                });

                //when & then 두 번째 호출
                thenThrownBy(() -> deliveryService.completeDelivery(order.getOrderNumber()))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 배송 완료된 주문입니다. OrderStatus: " + OrderStatus.DELIVERED);
            }
        }
    }
}