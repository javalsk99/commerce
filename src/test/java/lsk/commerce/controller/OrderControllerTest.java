package lsk.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lsk.commerce.config.WebConfig;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.domain.product.Album;
import lsk.commerce.dto.OrderProductDto;
import lsk.commerce.dto.request.OrderChangeRequest;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.request.OrderProductRequest;
import lsk.commerce.dto.response.OrderCancelResponse;
import lsk.commerce.dto.response.OrderChangeResponse;
import lsk.commerce.dto.response.OrderSearchResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.exception.GlobalExceptionHandler;
import lsk.commerce.query.OrderQueryService;
import lsk.commerce.query.dto.OrderProductQueryDto;
import lsk.commerce.query.dto.OrderQueryDto;
import lsk.commerce.query.dto.OrderSearchCond;
import lsk.commerce.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OrderController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
)
class OrderControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    OrderQueryService orderQueryService;

    private abstract class SetUp {

        @BeforeEach
        void beforeEach() throws Exception {
            HandlerMethodArgumentResolver resolver = mock(HandlerMethodArgumentResolver.class);
            given(resolver.supportsParameter(any())).willReturn(true);
            given(resolver.resolveArgument(any(), any(), any(), any())).willReturn("id_A");

            mvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService, orderQueryService))
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .setCustomArgumentResolvers(resolver)
                    .build();
        }
    }

    @Nested
    class Create extends SetUp {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Album album1 = createAlbum1("BANG BANG");
                Album album2 = createAlbum1("BLACKHOLE");

                OrderProductRequest orderProductRequest1 = new OrderProductRequest(album1.getProductNumber(), 3);
                OrderProductRequest orderProductRequest2 = new OrderProductRequest(album2.getProductNumber(), 2);
                List<OrderProductRequest> orderProductRequestList = List.of(orderProductRequest1, orderProductRequest2);
                OrderCreateRequest request = new OrderCreateRequest(orderProductRequestList);
                String json = objectMapper.writeValueAsString(request);

                String orderNumber = "dn39chfus9cu";

                given(orderService.order(any(OrderCreateRequest.class), anyString())).willReturn(orderNumber);

                //when & then
                mvc.perform(post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data").value(orderNumber))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                then(orderService).should().order(request, "id_A");
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidCreateRequestProvider")
            void invalidInput(OrderCreateRequest request) throws Exception {
                //given
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                then(orderService).should(never()).order(any(), any());
            }

            @Test
            void order_Failed_ProductNotFound() throws Exception {
                //given
                OrderProductRequest orderProductRequest = new OrderProductRequest("llIIllII00OO", 4);
                OrderCreateRequest request = new OrderCreateRequest(List.of(orderProductRequest));
                String json = objectMapper.writeValueAsString(request);

                given(orderService.order(any(OrderCreateRequest.class), anyString())).willThrow(new DataNotFoundException("존재하지 않는 상품입니다"));

                //when & then
                mvc.perform(post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 상품입니다"))
                        .andDo(print());

                //then
                then(orderService).should().order(request, "id_A");
            }

            static Stream<Arguments> invalidCreateRequestProvider() {
                String productNumber = "fji36nc7xk3b";

                return Stream.of(
                        argumentSet("OrderProductRequestList null", new OrderCreateRequest(null)),
                        argumentSet("OrderProductRequestList empty", new OrderCreateRequest(Collections.emptyList())),
                        argumentSet("productNumber 빈 문자열", new OrderCreateRequest(List.of(new OrderProductRequest(null, 3)))),
                        argumentSet("productNumber 빈 문자열", new OrderCreateRequest(List.of(new OrderProductRequest("", 3)))),
                        argumentSet("productNumber 공백", new OrderCreateRequest(List.of(new OrderProductRequest(" ".repeat(12), 3)))),
                        argumentSet("productNumber 12자 미만", new OrderCreateRequest(List.of(new OrderProductRequest("l".repeat(11), 3)))),
                        argumentSet("productNumber 12자 초과", new OrderCreateRequest(List.of(new OrderProductRequest("l".repeat(13), 3)))),
                        argumentSet("quantity null", new OrderCreateRequest(List.of(new OrderProductRequest(productNumber, null)))),
                        argumentSet("quantity 100 초과", new OrderCreateRequest(List.of(new OrderProductRequest(productNumber, 101))))
                );
            }
        }
    }

    @Nested
    class OrderList {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Order order1 = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                Order order2 = createOrder2();
                String orderNumber1 = order1.getOrderNumber();
                String orderNumber2 = order2.getOrderNumber();

                MultiValueMap<String, String> cond = new LinkedMultiValueMap<>();
                cond.add("loginId", "a");
                cond.add("orderStatus", OrderStatus.CREATED.name());

                OrderSearchResponse orderSearchResponse1 = new OrderSearchResponse(orderNumber1, 115000, OrderStatus.CREATED);
                OrderSearchResponse orderSearchResponse2 = new OrderSearchResponse(orderNumber2, 30000, OrderStatus.CREATED);
                List<OrderSearchResponse> orderSearchResponseList = List.of(orderSearchResponse1, orderSearchResponse2);

                given(orderQueryService.searchOrders(any(OrderSearchCond.class))).willReturn(orderSearchResponseList);

                //when & then
                mvc.perform(get("/orders")
                                .params(cond))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].orderNumber").value(orderNumber1))
                        .andExpect(jsonPath("$.data[0].totalAmount").value(115000))
                        .andExpect(jsonPath("$.data[0].orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data[1].orderNumber").value(orderNumber2))
                        .andExpect(jsonPath("$.data[1].totalAmount").value(30000))
                        .andExpect(jsonPath("$.data[1].orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.count").value(2))
                        .andDo(print());

                //then
                then(orderQueryService).should().searchOrders(any(OrderSearchCond.class));
            }

            @Test
            void notFound() throws Exception {
                //given
                MultiValueMap<String, String> cond = new LinkedMultiValueMap<>();
                cond.add("orderStatus", OrderStatus.DELIVERED.name());
                cond.add("deliveryStatus", DeliveryStatus.PREPARING.name());

                given(orderQueryService.searchOrders(any(OrderSearchCond.class))).willReturn(Collections.emptyList());

                //when & then
                mvc.perform(get("/orders")
                                .params(cond))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").isEmpty())
                        .andExpect(jsonPath("$.count").value(0))
                        .andDo(print());

                //then
                then(orderQueryService).should().searchOrders(any(OrderSearchCond.class));
            }
        }
    }

    @Nested
    class FindOrder extends SetUp {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();

                OrderQueryDto orderQueryDto = getOrderQueryDto(orderNumber);

                given(orderQueryService.findOrder(anyString(), anyString())).willReturn(orderQueryDto);

                //when & then
                mvc.perform(get("/orders/{orderNumber}", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderNumber").value(orderNumber))
                        .andExpect(jsonPath("$.data.orderProductQueryDtoList[0].name").value("BANG BANG"))
                        .andExpect(jsonPath("$.data.orderProductQueryDtoList[1].name").value("BLACKHOLE"))
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.WAITING.name()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                then(orderQueryService).should().findOrder(orderNumber, "id_A");
            }

            private static OrderQueryDto getOrderQueryDto(String orderNumber) {
                OrderProductQueryDto orderProductQueryDto1 = new OrderProductQueryDto(orderNumber, "BANG BANG", 15000, 3, 45000);
                OrderProductQueryDto orderProductQueryDto2 = new OrderProductQueryDto(orderNumber, "BLACKHOLE", 15000, 4, 60000);

                return OrderQueryDto.builder()
                        .loginId("id_A")
                        .orderNumber(orderNumber)
                        .orderProductQueryDtoList(List.of(orderProductQueryDto1, orderProductQueryDto2))
                        .totalAmount(105000)
                        .orderStatus(OrderStatus.CREATED)
                        .deliveryStatus(DeliveryStatus.WAITING)
                        .build();
            }
        }

        @Nested
        class FailureCase {

            @Test
            void findOrder_Failed_OrderNotFound() throws Exception {
                //given
                given(orderQueryService.findOrder(anyString(), anyString())).willThrow(new DataNotFoundException("존재하지 않는 주문입니다"));

                //when & then
                mvc.perform(get("/orders/{orderNumber}", "lllIIIll00OO"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 주문입니다"))
                        .andDo(print());

                //then
                then(orderQueryService).should().findOrder("lllIIIll00OO", "id_A");
            }
        }
    }

    @Nested
    class ChangeOrder extends SetUp {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Album album = createAlbum1("BLACKHOLE");
                OrderProductRequest orderProductRequest = new OrderProductRequest(album.getProductNumber(), 5);
                OrderChangeRequest request = new OrderChangeRequest(List.of(orderProductRequest));
                String json = objectMapper.writeValueAsString(request);

                Order order = createOrder1(Map.of("BLACKHOLE", 5));
                String orderNumber = order.getOrderNumber();
                OrderProductDto orderProductDto = new OrderProductDto("BLACKHOLE", 15000, 5, 75000);
                OrderChangeResponse orderChangeResponse = new OrderChangeResponse(List.of(orderProductDto), 75000);

                given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(order);
                given(orderService.getOrderChangeResponse(any(Order.class))).willReturn(orderChangeResponse);

                //when & then
                mvc.perform(patch("/orders/{orderNumber}", orderNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].name").value("BLACKHOLE"))
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].price").value(15000))
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].quantity").value(5))
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].orderPrice").value(75000))
                        .andExpect(jsonPath("$.data.totalAmount").value(75000))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> then(orderService).should().changeOrder(orderNumber, request, "id_A"));
                    softly.check(() -> then(orderService).should().findOrderWithAllExceptMember(orderNumber));
                    softly.check(() -> then(orderService).should().getOrderChangeResponse(order));
                });
            }

            @Test
            void idempotency() throws Exception {
                //given
                Album album = createAlbum1("BLACKHOLE");
                OrderProductRequest orderProductRequest = new OrderProductRequest(album.getProductNumber(), 5);
                OrderChangeRequest request = new OrderChangeRequest(List.of(orderProductRequest));
                String json = objectMapper.writeValueAsString(request);

                Order order = createOrder1(Map.of("BLACKHOLE", 5));
                String orderNumber = order.getOrderNumber();
                OrderProductDto orderProductDto = new OrderProductDto("BLACKHOLE", 15000, 5, 75000);
                OrderChangeResponse orderChangeResponse = new OrderChangeResponse(List.of(orderProductDto), 75000);

                given(orderService.findOrderWithAllExceptMember(anyString())).willReturn(order);
                given(orderService.getOrderChangeResponse(any(Order.class))).willReturn(orderChangeResponse);

                //when & then 첫 번째 요청
                mvc.perform(patch("/orders/{orderNumber}", orderNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].name").value("BLACKHOLE"))
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].price").value(15000))
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].quantity").value(5))
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].orderPrice").value(75000))
                        .andExpect(jsonPath("$.data.totalAmount").value(75000))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> then(orderService).should().changeOrder(orderNumber, request, "id_A"));
                    softly.check(() -> then(orderService).should().findOrderWithAllExceptMember(orderNumber));
                    softly.check(() -> then(orderService).should().getOrderChangeResponse(order));
                });

                //when & then 두 번째 요청
                mvc.perform(patch("/orders/{orderNumber}", orderNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].name").value("BLACKHOLE"))
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].price").value(15000))
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].quantity").value(5))
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].orderPrice").value(75000))
                        .andExpect(jsonPath("$.data.totalAmount").value(75000))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> then(orderService).should(times(2)).changeOrder(orderNumber, request, "id_A"));
                    softly.check(() -> then(orderService).should(times(2)).findOrderWithAllExceptMember(orderNumber));
                    softly.check(() -> then(orderService).should(times(2)).getOrderChangeResponse(order));
                });
            }
        }

        @Nested
        class FailureCase {

            @ParameterizedTest
            @MethodSource("invalidChangeRequestProvider")
            void invalidInput(OrderChangeRequest request) throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();

                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(patch("/orders/{orderNumber}", orderNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isBadRequest())
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> then(orderService).should(never()).changeOrder(any(), any(), any()));
                    softly.check(() -> then(orderService).should(never()).findOrderWithAllExceptMember(any()));
                    softly.check(() -> then(orderService).should(never()).getOrderChangeResponse(any()));
                });
            }

            @Test
            void changeOrder_Failed_ProductNotFound() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();
                OrderProductRequest orderProductRequest = new OrderProductRequest("lllIIIll00OO", 3);
                OrderChangeRequest request = new OrderChangeRequest(List.of(orderProductRequest));
                String json = objectMapper.writeValueAsString(request);

                willThrow(new DataNotFoundException("존재하지 않는 상품입니다")).given(orderService).changeOrder(anyString(), any(OrderChangeRequest.class), anyString());

                //when & then
                mvc.perform(patch("/orders/{orderNumber}", orderNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 상품입니다"))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> then(orderService).should().changeOrder(orderNumber, request, "id_A"));
                    softly.check(() -> then(orderService).should(never()).findOrderWithAllExceptMember(any()));
                    softly.check(() -> then(orderService).should(never()).getOrderChangeResponse(any()));
                });
            }

            static Stream<Arguments> invalidChangeRequestProvider() {
                String productNumber = "fji36nc7xk3b";

                return Stream.of(
                        argumentSet("OrderProductRequestList null", new OrderChangeRequest(null)),
                        argumentSet("OrderProductRequestList empty", new OrderChangeRequest(Collections.emptyList())),
                        argumentSet("productNumber 빈 문자열", new OrderChangeRequest(List.of(new OrderProductRequest(null, 3)))),
                        argumentSet("productNumber 빈 문자열", new OrderChangeRequest(List.of(new OrderProductRequest("", 3)))),
                        argumentSet("productNumber 공백", new OrderChangeRequest(List.of(new OrderProductRequest(" ".repeat(12), 3)))),
                        argumentSet("productNumber 12자 미만", new OrderChangeRequest(List.of(new OrderProductRequest("l".repeat(11), 3)))),
                        argumentSet("productNumber 12자 초과", new OrderChangeRequest(List.of(new OrderProductRequest("l".repeat(13), 3)))),
                        argumentSet("quantity null", new OrderChangeRequest(List.of(new OrderProductRequest(productNumber, null)))),
                        argumentSet("quantity 100 초과", new OrderChangeRequest(List.of(new OrderProductRequest(productNumber, 101))))
                );
            }
        }
    }

    @Nested
    class Delete extends SetUp {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();

                //when & then
                mvc.perform(delete("/orders/{orderNumber}", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                then(orderService).should().deleteOrder(orderNumber, "id_A");
            }

            @Test
            void idempotency() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();

                //when & then 첫 번째 요청
                mvc.perform(delete("/orders/{orderNumber}", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                then(orderService).should().deleteOrder(orderNumber, "id_A");

                //when & then 두 번째 요청
                mvc.perform(delete("/orders/{orderNumber}", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                then(orderService).should(times(2)).deleteOrder(orderNumber, "id_A");
            }
        }
    }

    @Nested
    class CancelOrder extends SetUp {

        @Nested
        class SuccessCase {

            @Test
            void hasNoPayment() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();
                OrderCancelResponse orderCancelResponse = new OrderCancelResponse(OrderStatus.CANCELED, null, DeliveryStatus.CANCELED);

                given(orderService.cancelOrder(anyString(), anyString())).willReturn(order);
                given(orderService.getOrderCancelResponse(any(Order.class))).willReturn(orderCancelResponse);

                //when & then
                mvc.perform(patch("/orders/{orderNumber}/cancel", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CANCELED.name()))
                        .andExpect(jsonPath("$.data.paymentStatus").value(nullValue()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.CANCELED.name()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> then(orderService).should().cancelOrder(orderNumber, "id_A"));
                    softly.check(() -> then(orderService).should().getOrderCancelResponse(order));
                });
            }

            @Test
            void hasPayment() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();
                OrderCancelResponse orderCancelResponse = new OrderCancelResponse(OrderStatus.CANCELED, PaymentStatus.CANCELED, DeliveryStatus.CANCELED);

                given(orderService.cancelOrder(anyString(), anyString())).willReturn(order);
                given(orderService.getOrderCancelResponse(any(Order.class))).willReturn(orderCancelResponse);

                //when & then
                mvc.perform(patch("/orders/{orderNumber}/cancel", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CANCELED.name()))
                        .andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.CANCELED.name()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.CANCELED.name()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> then(orderService).should().cancelOrder(orderNumber, "id_A"));
                    softly.check(() -> then(orderService).should().getOrderCancelResponse(order));
                });
            }

            @Test
            void idempotency() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();
                OrderCancelResponse orderCancelResponse = new OrderCancelResponse(OrderStatus.CANCELED, PaymentStatus.CANCELED, DeliveryStatus.CANCELED);

                given(orderService.cancelOrder(anyString(), anyString())).willReturn(order);
                given(orderService.getOrderCancelResponse(any(Order.class))).willReturn(orderCancelResponse);

                //when & then 첫 번째 요청
                mvc.perform(patch("/orders/{orderNumber}/cancel", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CANCELED.name()))
                        .andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.CANCELED.name()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.CANCELED.name()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> then(orderService).should().cancelOrder(orderNumber, "id_A"));
                    softly.check(() -> then(orderService).should().getOrderCancelResponse(order));
                });

                //when & then 두 번째 요청
                mvc.perform(patch("/orders/{orderNumber}/cancel", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CANCELED.name()))
                        .andExpect(jsonPath("$.data.paymentStatus").value(PaymentStatus.CANCELED.name()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.CANCELED.name()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> then(orderService).should(times(2)).cancelOrder(orderNumber, "id_A"));
                    softly.check(() -> then(orderService).should(times(2)).getOrderCancelResponse(order));
                });
            }
        }
    }

    private Order createOrder1(Map<String, Integer> productMap) {
        Member member = Member.builder()
                .name("UserA")
                .loginId("id_A")
                .password("abAB12!@")
                .zipcode("01234")
                .baseAddress("서울시 강남구")
                .detailAddress("101동 101호")
                .build();
        Delivery delivery = new Delivery(member);

        List<OrderProduct> orderProducts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : productMap.entrySet()) {
            Album album = createAlbum1(entry.getKey());
            OrderProduct orderProduct = OrderProduct.createOrderProduct(album, entry.getValue());
            orderProducts.add(orderProduct);
        }

        return Order.createOrder(member, delivery, orderProducts);
    }

    private Order createOrder2() {
        Member member = Member.builder()
                .name("UserA")
                .loginId("id_A")
                .password("abAB12!@")
                .zipcode("01234")
                .baseAddress("서울시 강남구")
                .detailAddress("101동 101호")
                .build();
        Delivery delivery = new Delivery(member);
        Album album = createAlbum2();

        OrderProduct orderProduct = OrderProduct.createOrderProduct(album, 2);
        return Order.createOrder(member, delivery, List.of(orderProduct));
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

    private Album createAlbum2() {
        return Album.builder()
                .name("타임 캡슐")
                .price(15000)
                .stockQuantity(10)
                .artist("다비치")
                .studio("씨에이엠위더스")
                .build();
    }
}