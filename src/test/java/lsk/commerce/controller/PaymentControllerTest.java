package lsk.commerce.controller;

import io.portone.sdk.server.webhook.WebhookVerifier;
import lsk.commerce.api.portone.CompletePaymentRequest;
import lsk.commerce.api.portone.SyncPaymentException;
import lsk.commerce.api.portone.SyncPaymentExceptionHandler;
import lsk.commerce.config.WebConfig;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.domain.product.Album;
import lsk.commerce.dto.OrderProductDto;
import lsk.commerce.dto.request.PaymentCompleteResponse;
import lsk.commerce.dto.response.OrderPaymentResponse;
import lsk.commerce.dto.response.OrderResponse;
import lsk.commerce.dto.response.PaymentResponse;
import lsk.commerce.exception.GlobalExceptionHandler;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.PaymentService;
import lsk.commerce.service.PaymentSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.mock;

@WebMvcTest(
        controllers = PaymentController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
)
@AutoConfigureWebTestClient
class PaymentControllerTest {

    @Autowired
    WebTestClient client;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    WebhookVerifier portoneWebhook;

    @MockitoBean
    PaymentService paymentService;

    @MockitoBean
    PaymentSyncService paymentSyncService;

    private abstract class SetUp {

        @BeforeEach
        void beforeEach() throws Exception {
            HandlerMethodArgumentResolver resolver = mock(HandlerMethodArgumentResolver.class);
            given(resolver.supportsParameter(any())).willReturn(true);
            given(resolver.resolveArgument(any(), any(), any())).willReturn(Mono.just("id_A"));

            client = WebTestClient.bindToController(new PaymentController(portoneWebhook, orderService, paymentService, paymentSyncService))
                    .argumentResolvers(configurer -> configurer.addCustomResolver(resolver))
                    .controllerAdvice(new GlobalExceptionHandler(), new SyncPaymentExceptionHandler())
                    .build();
        }
    }

    @Nested
    class RequestPayment extends SetUp {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Order order = createOrder();
                String orderNumber = order.getOrderNumber();

                Payment.requestPayment(order);
                PaymentResponse paymentResponse = new PaymentResponse(order.getPayment().getPaymentAmount(), order.getPayment().getPaymentStatus(), order.getOrderStatus(), order.getOrderDate(), order.getDelivery().getDeliveryStatus());

                given(paymentService.request(anyString(), anyString())).willReturn(order.getPayment());
                given(paymentService.getPaymentResponse(any(Payment.class))).willReturn(paymentResponse);

                //when & then
                client.post().uri("/payments/orders/{orderNumber}", orderNumber)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$.data.totalAmount").isEqualTo(75000)
                        .jsonPath("$.data.paymentStatus").isEqualTo(PaymentStatus.PENDING.name())
                        .jsonPath("$.data.orderStatus").isEqualTo(OrderStatus.CREATED.name())
                        .jsonPath("$.data.deliveryStatus").isEqualTo(DeliveryStatus.WAITING.name())
                        .jsonPath("$.count").isEqualTo(1)
                        .consumeWith(System.out::println);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(paymentService).should().request(orderNumber, "id_A"));
                    softly.check(() -> BDDMockito.then(paymentService).should().getPaymentResponse(order.getPayment()));
                });
            }
        }

        @Nested
        class FailureCase {

            @Test
            void request_Failed_AlreadyRequest() {
                //given
                Order order = createOrder();
                String orderNumber = order.getOrderNumber();

                Payment.requestPayment(order);
                PaymentResponse paymentResponse = new PaymentResponse(order.getPayment().getPaymentAmount(), order.getPayment().getPaymentStatus(), order.getOrderStatus(), order.getOrderDate(), order.getDelivery().getDeliveryStatus());

                given(paymentService.request(anyString(), anyString()))
                        .willReturn(order.getPayment())
                        .willThrow(new IllegalStateException("이미 결제 정보가 있습니다"));
                given(paymentService.getPaymentResponse(any(Payment.class))).willReturn(paymentResponse);

                //when & then 첫 번째 요청
                client.post().uri("/payments/orders/{orderNumber}", orderNumber)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$.data.totalAmount").isEqualTo(75000)
                        .jsonPath("$.data.paymentStatus").isEqualTo(PaymentStatus.PENDING.name())
                        .jsonPath("$.data.orderStatus").isEqualTo(OrderStatus.CREATED.name())
                        .jsonPath("$.data.deliveryStatus").isEqualTo(DeliveryStatus.WAITING.name())
                        .jsonPath("$.count").isEqualTo(1)
                        .consumeWith(System.out::println);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(paymentService).should().request(orderNumber, "id_A"));
                    softly.check(() -> BDDMockito.then(paymentService).should().getPaymentResponse(order.getPayment()));
                });

                //when & then 두 번째 요청
                client.post().uri("/payments/orders/{orderNumber}", orderNumber)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("BAD_STATUS")
                        .jsonPath("$.message").isEqualTo("이미 결제 정보가 있습니다")
                        .consumeWith(System.out::println);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(paymentService).should(times(2)).request(orderNumber, "id_A"));
                    softly.check(() -> BDDMockito.then(paymentService).should().getPaymentResponse(any()));
                });
            }
        }

        private Order createOrder() {
            Member member = Member.builder()
                    .name("UserA")
                    .loginId("id_A")
                    .password("00000000")
                    .city("Seoul")
                    .street("Gangnam")
                    .zipcode("01234")
                    .build();
            Delivery delivery = new Delivery(member);

            Album album1 = createAlbum1("BANG BANG");
            Album album2 = createAlbum1("BLACKHOLE");
            OrderProduct orderProduct1 = OrderProduct.createOrderProduct(album1, 3);
            OrderProduct orderProduct2 = OrderProduct.createOrderProduct(album2, 2);
            return Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
        }

        private Album createAlbum1(String name) {
            return Album.builder()
                    .name(name)
                    .price(15000)
                    .stockQuantity(10)
                    .artist("IVE")
                    .studio("STARSHIP")
                    .build();
        }

        private static OrderResponse getOrderResponse() {
            OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 3, 45000);
            OrderProductDto orderProductDto2 = new OrderProductDto("BLACKHOLE", 15000, 4, 60000);
            return new OrderResponse(List.of(orderProductDto1, orderProductDto2), 105000, OrderStatus.CREATED, LocalDateTime.now(),
                    PaymentStatus.PENDING, null, DeliveryStatus.WAITING, null, null);
        }
    }

    @Nested
    class GetOrder extends SetUp {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                Order order = createOrder();
                String orderNumber = order.getOrderNumber();

                OrderPaymentResponse orderPaymentResponse = getOrderPaymentResponse(orderNumber);

                given(orderService.findOrderWithAll(anyString())).willReturn(order);
                given(orderService.getOrderPaymentResponse(any(Order.class))).willReturn(orderPaymentResponse);

                //when & then
                client.get().uri("/api/payments/{orderNumber}", orderNumber)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$.data.orderStatus").isEqualTo(OrderStatus.CREATED.name())
                        .jsonPath("$.data.paymentStatus").isEqualTo(PaymentStatus.PENDING.name())
                        .consumeWith(System.out::println);

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithAll(orderNumber));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderPaymentResponse(order));
                });
            }

            private static OrderPaymentResponse getOrderPaymentResponse(String orderNumber) {
                OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 3, 45000);
                OrderProductDto orderProductDto2 = new OrderProductDto("BLACKHOLE", 15000, 4, 60000);

                String memberLoginId = "id_A";
                String paymentId = "vndu867sbci3";
                return new OrderPaymentResponse(memberLoginId,
                        orderNumber, List.of(orderProductDto1, orderProductDto2), 105000, OrderStatus.CREATED, LocalDateTime.now(),
                        paymentId, PaymentStatus.PENDING, DeliveryStatus.WAITING);
            }
        }
    }

    @Nested
    class CompletePayment extends SetUp {

        @Nested
        class SuccessCase {

            @Test
            void basic() {
                //given
                String paymentId = "vndu867sbci3";
                CompletePaymentRequest request = new CompletePaymentRequest(paymentId);

                PaymentCompleteResponse paymentCompleteResponse = new PaymentCompleteResponse(paymentId, PaymentStatus.COMPLETED);

                given(paymentSyncService.syncPayment(anyString(), anyString())).willReturn(Mono.just(paymentCompleteResponse));

                //when & then
                client.post().uri("/api/payments/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$.data.paymentStatus").isEqualTo(PaymentStatus.COMPLETED.name())
                        .consumeWith(System.out::println);

                //then
                BDDMockito.then(paymentSyncService).should().syncPayment(paymentId, "id_A");
            }
        }

        @Nested
        class FailureCase {

            @Test
            void syncPayment_Failed_NotFound() {
                //given
                String paymentId = "lllIIIll00OO";
                CompletePaymentRequest request = new CompletePaymentRequest(paymentId);

                given(paymentSyncService.syncPayment(anyString(), anyString())).willThrow(new SyncPaymentException("결제 정보 조회 중 오류 발생"));

                //when & then
                client.post().uri("/api/payments/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody()
                        .jsonPath("$.code").isEqualTo("PORTONE_ERROR")
                        .jsonPath("$.message").isEqualTo("결제 처리 중 오류가 발생했습니다. 잠시만 기다려 주세요")
                        .consumeWith(System.out::println);

                //then
                BDDMockito.then(paymentSyncService).should().syncPayment(paymentId, "id_A");
            }
        }
    }

    private Order createOrder() {
        Member member = Member.builder()
                .name("UserA")
                .loginId("id_A")
                .password("00000000")
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
                .build();
        Delivery delivery = new Delivery(member);
        Album album1 = createAlbum("BANG BANG");
        Album album2 = createAlbum("BLACKHOLE");

        OrderProduct orderProduct1 = OrderProduct.createOrderProduct(album1, 3);
        OrderProduct orderProduct2 = OrderProduct.createOrderProduct(album2, 4);
        return Order.createOrder(member, delivery, List.of(orderProduct1, orderProduct2));
    }

    private Album createAlbum(String name) {
        return Album.builder()
                .name(name)
                .price(15000)
                .stockQuantity(10)
                .artist("IVE")
                .studio("STARSHIP")
                .build();
    }
}