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
import lsk.commerce.dto.response.OrderResponse;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.query.OrderQueryService;
import lsk.commerce.query.dto.OrderProductQueryDto;
import lsk.commerce.query.dto.OrderQueryDto;
import lsk.commerce.query.dto.OrderSearchCond;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.PaymentService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
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
    PaymentService paymentService;

    @MockitoBean
    OrderQueryService orderQueryService;

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Album album1 = createAlbum1("BANG BANG");
                Album album2 = createAlbum1("BLACKHOLE");
                String productNumber1 = album1.getProductNumber();
                String productNumber2 = album2.getProductNumber();

                OrderCreateRequest request = new OrderCreateRequest("id_A", Map.of(productNumber1, 3, productNumber2, 2));
                String json = objectMapper.writeValueAsString(request);

                String orderNumber = "dn39chfus9cu";

                given(orderService.order(any(OrderCreateRequest.class))).willReturn(orderNumber);

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
                BDDMockito.then(orderService).should().order(request);
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
                BDDMockito.then(orderService).should(never()).order(any());
            }

            @Test
            void order_Failed_ProductNotFound() throws Exception {
                //given
                OrderCreateRequest request = new OrderCreateRequest("id_A", Map.of("llIIllII00OO", 4));
                String json = objectMapper.writeValueAsString(request);

                given(orderService.order(any(OrderCreateRequest.class))).willThrow(new DataNotFoundException("존재하지 않는 상품입니다"));

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
                BDDMockito.then(orderService).should().order(request);
            }

            static Stream<Arguments> invalidCreateRequestProvider() {
                String productNumber = "fji36nc7xk3b";
                Map<String, Integer> nullValueMap = new HashMap<>();
                nullValueMap.put(productNumber, null);

                return Stream.of(
                        argumentSet("memberLoginId null", new OrderCreateRequest(null, Map.of(productNumber, 3))),
                        argumentSet("productMap null", new OrderCreateRequest("id_A", null)),
                        argumentSet("productMap empty", new OrderCreateRequest("id_A", Collections.emptyMap())),
                        argumentSet("productMap key 빈 문자열", new OrderCreateRequest("id_A", Map.of("", 3))),
                        argumentSet("productMap key 공백", new OrderCreateRequest("id_A", Map.of(" ".repeat(12), 3))),
                        argumentSet("productMap key 12자 미만", new OrderCreateRequest("id_A", Map.of("l".repeat(11), 3))),
                        argumentSet("productMap key 12자 초과", new OrderCreateRequest("id_A", Map.of("l".repeat(13), 3))),
                        argumentSet("productMap value null", new OrderCreateRequest("id_A", nullValueMap)),
                        argumentSet("productMap value 100 초과", new OrderCreateRequest("id_A", Map.of(productNumber, 101))),
                        argumentSet("memberLoginId 공백", new OrderCreateRequest(" ", Map.of(productNumber, 3))),
                        argumentSet("memberLoginId 4자 미만", new OrderCreateRequest("a".repeat(3), Map.of(productNumber, 3))),
                        argumentSet("memberLoginId 20자 초과", new OrderCreateRequest("a".repeat(21), Map.of(productNumber, 3)))
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

                List<OrderQueryDto> orderQueryDtoList = getOrderQueryDtoList(orderNumber1, orderNumber2);

                given(orderQueryService.searchOrders(any(OrderSearchCond.class))).willReturn(orderQueryDtoList);

                //when & then
                mvc.perform(get("/orders")
                                .params(cond))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[0].loginId").doesNotExist())
                        .andExpect(jsonPath("$.data[0].orderNumber").value(orderNumber1))
                        .andExpect(jsonPath("$.data[0].orderProductQueryDtoList[0].name").value("BANG BANG"))
                        .andExpect(jsonPath("$.data[0].orderProductQueryDtoList[1].name").value("BLACKHOLE"))
                        .andExpect(jsonPath("$.data[0].orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data[0].deliveryStatus").value(DeliveryStatus.WAITING.name()))
                        .andExpect(jsonPath("$.data[1].loginId").doesNotExist())
                        .andExpect(jsonPath("$.data[1].orderNumber").value(orderNumber2))
                        .andExpect(jsonPath("$.data[1].orderProductQueryDtoList[0].name").value("타임 캡슐"))
                        .andExpect(jsonPath("$.data[1].orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data[1].deliveryStatus").value(DeliveryStatus.WAITING.name()))
                        .andExpect(jsonPath("$.count").value(2))
                        .andDo(print());

                //then
                BDDMockito.then(orderQueryService).should().searchOrders(any(OrderSearchCond.class));
            }

            private static List<OrderQueryDto> getOrderQueryDtoList(String orderNumber1, String orderNumber2) {
                OrderProductQueryDto orderProductQueryDto1 = new OrderProductQueryDto(orderNumber1, "BANG BANG", 15000, 3, 45000);
                OrderProductQueryDto orderProductQueryDto2 = new OrderProductQueryDto(orderNumber1, "BLACKHOLE", 15000, 4, 60000);
                OrderProductQueryDto orderProductQueryDto3 = new OrderProductQueryDto(orderNumber2, "타임 캡슐", 15000, 2, 30000);
                OrderQueryDto orderQueryDto1 = OrderQueryDto.builder()
                        .loginId("id_A")
                        .orderNumber(orderNumber1)
                        .orderProductQueryDtoList(List.of(orderProductQueryDto1, orderProductQueryDto2))
                        .totalAmount(105000)
                        .orderStatus(OrderStatus.CREATED)
                        .deliveryStatus(DeliveryStatus.WAITING)
                        .build();
                OrderQueryDto orderQueryDto2 = OrderQueryDto.builder()
                        .loginId("id_A")
                        .orderNumber(orderNumber2)
                        .orderProductQueryDtoList(List.of(orderProductQueryDto3))
                        .totalAmount(30000)
                        .orderStatus(OrderStatus.CREATED)
                        .deliveryStatus(DeliveryStatus.WAITING)
                        .build();

                return List.of(orderQueryDto1, orderQueryDto2);
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
                BDDMockito.then(orderQueryService).should().searchOrders(any(OrderSearchCond.class));
            }
        }
    }

    @Nested
    class FindOrder {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();

                OrderQueryDto orderQueryDto = getOrderQueryDto(orderNumber);

                given(orderQueryService.findOrder(orderNumber)).willReturn(orderQueryDto);

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
                BDDMockito.then(orderQueryService).should().findOrder(orderNumber);
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
                given(orderQueryService.findOrder(anyString())).willThrow(new DataNotFoundException("존재하지 않는 주문입니다"));

                //when & then
                mvc.perform(get("/orders/{orderNumber}", "lllIIIll00OO"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 주문입니다"))
                        .andDo(print());

                //then
                BDDMockito.then(orderQueryService).should().findOrder("lllIIIll00OO");
            }
        }
    }

    @Nested
    class ChangeOrder {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                Album album = createAlbum1("BLACKHOLE");
                String productNumber = album.getProductNumber();

                OrderChangeRequest request = new OrderChangeRequest(Map.of(productNumber, 5));
                String json = objectMapper.writeValueAsString(request);

                Order order = createOrder1(Map.of("BLACKHOLE", 5));
                String orderNumber = order.getOrderNumber();
                OrderResponse orderResponse = OrderResponse.from(order);

                given(orderService.findOrderWithDeliveryPayment(anyString())).willReturn(order);
                given(orderService.getOrderResponse(any(Order.class))).willReturn(orderResponse);

                //when & then
                mvc.perform(patch("/orders/{orderNumber}", orderNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].name").value("BLACKHOLE"))
                        .andExpect(jsonPath("$.data.totalAmount").value(75000))
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data.paymentStatus").value(nullValue()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.WAITING.name()))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should().changeOrder(orderNumber, request));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(orderNumber));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderResponse(order));
                });
            }

            @Test
            void idempotency() throws Exception {
                //given
                Album album = createAlbum1("BLACKHOLE");
                String productNumber = album.getProductNumber();

                OrderChangeRequest request = new OrderChangeRequest(Map.of(productNumber, 5));
                String json = objectMapper.writeValueAsString(request);

                Order order = createOrder1(Map.of("BLACKHOLE", 5));
                String orderNumber = order.getOrderNumber();
                OrderResponse orderResponse = OrderResponse.from(order);

                given(orderService.findOrderWithDeliveryPayment(anyString())).willReturn(order);
                given(orderService.getOrderResponse(any(Order.class))).willReturn(orderResponse);

                //when & then 첫 번째 요청
                mvc.perform(patch("/orders/{orderNumber}", orderNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].name").value("BLACKHOLE"))
                        .andExpect(jsonPath("$.data.totalAmount").value(75000))
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data.paymentStatus").value(nullValue()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.WAITING.name()))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should().changeOrder(orderNumber, request));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(orderNumber));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderResponse(order));
                });

                //when & then 두 번째 요청
                mvc.perform(patch("/orders/{orderNumber}", orderNumber)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderProductDtoList[0].name").value("BLACKHOLE"))
                        .andExpect(jsonPath("$.data.totalAmount").value(75000))
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data.paymentStatus").value(nullValue()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.WAITING.name()))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should(times(2)).changeOrder(orderNumber, request));
                    softly.check(() -> BDDMockito.then(orderService).should(times(2)).findOrderWithDeliveryPayment(orderNumber));
                    softly.check(() -> BDDMockito.then(orderService).should(times(2)).getOrderResponse(order));
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
                    softly.check(() -> BDDMockito.then(orderService).should(never()).changeOrder(any(), any()));
                    softly.check(() -> BDDMockito.then(orderService).should(never()).findOrderWithDeliveryPayment(any()));
                    softly.check(() -> BDDMockito.then(orderService).should(never()).getOrderResponse(any()));
                });
            }

            @Test
            void changeOrder_Failed_ProductNotFound() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();

                OrderChangeRequest request = new OrderChangeRequest(Map.of("lllIIIll00OO", 3));
                String json = objectMapper.writeValueAsString(request);

                willThrow(new DataNotFoundException("존재하지 않는 상품입니다")).given(orderService).changeOrder(anyString(), any(OrderChangeRequest.class));

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
                    softly.check(() -> BDDMockito.then(orderService).should().changeOrder(orderNumber, request));
                    softly.check(() -> BDDMockito.then(orderService).should(never()).findOrderWithDeliveryPayment(any()));
                    softly.check(() -> BDDMockito.then(orderService).should(never()).getOrderResponse(any()));
                });
            }

            static Stream<Arguments> invalidChangeRequestProvider() {
                String productNumber = "fji36nc7xk3b";
                Map<String, Integer> nullValueMap = new HashMap<>();
                nullValueMap.put(productNumber, null);

                return Stream.of(
                        argumentSet("productMap null", new OrderChangeRequest(null)),
                        argumentSet("productMap empty", new OrderChangeRequest(Collections.emptyMap())),
                        argumentSet("productMap key 빈 문자열", new OrderChangeRequest(Map.of("", 3))),
                        argumentSet("productMap key 공백", new OrderChangeRequest(Map.of(" ".repeat(12), 3))),
                        argumentSet("productMap key 12자 미만", new OrderChangeRequest(Map.of("l".repeat(11), 3))),
                        argumentSet("productMap key 12자 초과", new OrderChangeRequest(Map.of("l".repeat(13), 3))),
                        argumentSet("productMap value null", new OrderChangeRequest(nullValueMap)),
                        argumentSet("productMap value 100 초과", new OrderChangeRequest(Map.of(productNumber, 101)))
                );
            }
        }
    }

    @Nested
    class Delete {

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
                BDDMockito.then(orderService).should().deleteOrder(orderNumber);
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
                BDDMockito.then(orderService).should().deleteOrder(orderNumber);

                //when & then 두 번째 요청
                mvc.perform(delete("/orders/{orderNumber}", orderNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value("delete"))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(orderService).should(times(2)).deleteOrder(orderNumber);
            }
        }
    }

    @Nested
    class CancelOrder {

        @Nested
        class SuccessCase {

            @Test
            void hasNoPayment() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();

                OrderResponse orderResponse = getOrderResponseHasNoPayment();

                given(orderService.cancelOrder(anyString())).willReturn(order);
                given(orderService.getOrderResponse(any(Order.class))).willReturn(orderResponse);

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
                    softly.check(() -> BDDMockito.then(orderService).should().cancelOrder(orderNumber));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderResponse(order));
                });
            }

            @Test
            void hasPayment() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();

                OrderResponse orderResponse = getOrderResponseHasPayment();

                given(orderService.cancelOrder(anyString())).willReturn(order);
                given(orderService.getOrderResponse(any(Order.class))).willReturn(orderResponse);

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
                    softly.check(() -> BDDMockito.then(orderService).should().cancelOrder(orderNumber));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderResponse(order));
                });
            }

            @Test
            void idempotency() throws Exception {
                //given
                Order order = createOrder1(Map.of("BANG BANG", 3, "BLACKHOLE", 4));
                String orderNumber = order.getOrderNumber();

                OrderResponse orderResponse = getOrderResponseHasPayment();

                given(orderService.cancelOrder(anyString())).willReturn(order);
                given(orderService.getOrderResponse(any(Order.class))).willReturn(orderResponse);

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
                    softly.check(() -> BDDMockito.then(orderService).should().cancelOrder(orderNumber));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderResponse(order));
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
                    softly.check(() -> BDDMockito.then(orderService).should(times(2)).cancelOrder(orderNumber));
                    softly.check(() -> BDDMockito.then(orderService).should(times(2)).getOrderResponse(order));
                });
            }
        }

        private static OrderResponse getOrderResponseHasNoPayment() {
            OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 3, 45000);
            OrderProductDto orderProductDto2 = new OrderProductDto("BLACKHOLE", 15000, 4, 60000);
            return new OrderResponse(List.of(orderProductDto1, orderProductDto2), 105000, OrderStatus.CANCELED, LocalDateTime.now(),
                    null, null, DeliveryStatus.CANCELED, null, null);
        }

        private static OrderResponse getOrderResponseHasPayment() {
            OrderProductDto orderProductDto1 = new OrderProductDto("BANG BANG", 15000, 3, 45000);
            OrderProductDto orderProductDto2 = new OrderProductDto("BLACKHOLE", 15000, 4, 60000);
            return new OrderResponse(List.of(orderProductDto1, orderProductDto2), 105000, OrderStatus.CANCELED, LocalDateTime.now(),
                    PaymentStatus.CANCELED, null, DeliveryStatus.CANCELED, null, null);
        }
    }

    private Order createOrder1(Map<String, Integer> productMap) {
        Member member = Member.builder()
                .name("UserA")
                .loginId("id_A")
                .password("00000000")
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
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
                .password("00000000")
                .city("Seoul")
                .street("Gangnam")
                .zipcode("01234")
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