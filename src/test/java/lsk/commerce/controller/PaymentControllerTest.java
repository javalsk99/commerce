package lsk.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PaymentController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
)
class PaymentControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

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
            given(resolver.resolveArgument(any(), any(), any(), any())).willReturn("id_A");

            mvc = MockMvcBuilders.standaloneSetup(new PaymentController(portoneWebhook, orderService, paymentService, paymentSyncService))
                    .setControllerAdvice(new GlobalExceptionHandler(), new SyncPaymentExceptionHandler())
                    .setCustomArgumentResolvers(resolver)
                    .build();
        }
    }

    @Nested
    class RequestPayment extends SetUp {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Order order = createOrder();
                String orderNumber = order.getOrderNumber();

                Payment.requestPayment(order);
                PaymentResponse paymentResponse = new PaymentResponse(order.getPayment().getPaymentAmount(), order.getPayment().getPaymentStatus(), order.getOrderStatus(), order.getOrderDate(), order.getDelivery().getDeliveryStatus());

                given(paymentService.request(anyString(), anyString())).willReturn(order.getPayment());
                given(paymentService.getPaymentResponse(any(Payment.class))).willReturn(paymentResponse);

                //when & then
                mvc.perform(post("/payments/orders/{orderNumber}", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalAmount").value(75000))
                        .andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.PENDING.name()))
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.WAITING.name()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

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
            void request_Failed_AlreadyRequest() throws Exception {
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
                mvc.perform(post("/payments/orders/{orderNumber}", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalAmount").value(75000))
                        .andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.PENDING.name()))
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.WAITING.name()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(paymentService).should().request(orderNumber, "id_A"));
                    softly.check(() -> BDDMockito.then(paymentService).should().getPaymentResponse(order.getPayment()));
                });

                //when & then 두 번째 요청
                mvc.perform(post("/payments/orders/{orderNumber}", orderNumber))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("BAD_STATUS"))
                        .andExpect(jsonPath("$.message").value("이미 결제 정보가 있습니다"))
                        .andDo(print());

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
    }

    @Nested
    class GetOrder extends SetUp {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Order order = createOrder();
                String orderNumber = order.getOrderNumber();

                OrderPaymentResponse orderPaymentResponse = getOrderPaymentResponse(orderNumber);

                given(orderService.findOrderWithAll(anyString())).willReturn(order);
                given(orderService.getOrderPaymentResponse(any(Order.class))).willReturn(orderPaymentResponse);

                //when & then
                mvc.perform(get("/api/payments/{orderNumber}", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.PENDING.name()))
                        .andDo(print());

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
            void basic() throws Exception {
                //given
                String paymentId = "vndu867sbci3";
                CompletePaymentRequest request = new CompletePaymentRequest(paymentId);
                String json = objectMapper.writeValueAsString(request);

                PaymentCompleteResponse paymentCompleteResponse = new PaymentCompleteResponse(paymentId, PaymentStatus.COMPLETED);

                given(paymentSyncService.syncPayment(anyString(), anyString())).willReturn(Mono.just(paymentCompleteResponse));

                //when & then
                MvcResult mvcResult = mvc.perform(post("/api/payments/complete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(request().asyncStarted())
                        .andReturn();

                //then
                mvc.perform(asyncDispatch(mvcResult))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.COMPLETED.name()))
                        .andDo(print());

                BDDMockito.then(paymentSyncService).should().syncPayment(paymentId, "id_A");
            }
        }

        @Nested
        class FailureCase {

            @Test
            void syncPayment_Failed_NotFound() throws Exception {
                //given
                String paymentId = "lllIIIll00OO";
                CompletePaymentRequest request = new CompletePaymentRequest(paymentId);
                String json = objectMapper.writeValueAsString(request);

                given(paymentSyncService.syncPayment(anyString(), anyString())).willThrow(new SyncPaymentException("결제 정보 조회 중 오류 발생"));

                //when & then
                mvc.perform(post("/api/payments/complete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("PORTONE_ERROR"))
                        .andExpect(jsonPath("$.message").value("결제 처리 중 오류가 발생했습니다. 잠시만 기다려 주세요"))
                        .andDo(print());

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