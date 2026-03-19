package lsk.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lsk.commerce.config.WebConfig;
import lsk.commerce.domain.Delivery;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Member;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderProduct;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.product.Album;
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
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    String productNumber1 = "fji36nc7xk3b";
    String productNumber2 = "dbe3b6v221bu";
    String orderNumber1 = "dfbu398xueos";
    String orderNumber2 = "nb9d78ch93m4";

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                OrderCreateRequest request = new OrderCreateRequest("id_A", Map.of(productNumber1, 3, productNumber2, 2));
                String json = objectMapper.writeValueAsString(request);

                given(orderService.order(any(OrderCreateRequest.class))).willReturn(orderNumber1);

                //when & then
                mvc.perform(post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data").value(orderNumber1))
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

                given(orderService.order(any(OrderCreateRequest.class))).willThrow(new DataNotFoundException("존재하지 않는 상품입니다."));

                //when & then
                mvc.perform(post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 상품입니다."))
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
                MultiValueMap<String, String> cond = new LinkedMultiValueMap<>();
                cond.add("loginId", "a");
                cond.add("orderStatus", OrderStatus.CREATED.name());

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

                given(orderQueryService.searchOrders(any(OrderSearchCond.class))).willReturn(List.of(orderQueryDto1, orderQueryDto2));

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
                OrderProductQueryDto orderProductQueryDto1 = new OrderProductQueryDto(orderNumber1, "BANG BANG", 15000, 3, 45000);
                OrderProductQueryDto orderProductQueryDto2 = new OrderProductQueryDto(orderNumber1, "BLACKHOLE", 15000, 4, 60000);
                OrderQueryDto orderQueryDto = OrderQueryDto.builder()
                        .loginId("id_A")
                        .orderNumber(orderNumber1)
                        .orderProductQueryDtoList(List.of(orderProductQueryDto1, orderProductQueryDto2))
                        .totalAmount(105000)
                        .orderStatus(OrderStatus.CREATED)
                        .deliveryStatus(DeliveryStatus.WAITING)
                        .build();

                given(orderQueryService.findOrder(orderNumber1)).willReturn(orderQueryDto);

                //when & then
                mvc.perform(get("/orders/{orderNumber}", orderNumber1))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.orderNumber").value(orderNumber1))
                        .andExpect(jsonPath("$.data.orderProductQueryDtoList[0].name").value("BANG BANG"))
                        .andExpect(jsonPath("$.data.orderProductQueryDtoList[1].name").value("BLACKHOLE"))
                        .andExpect(jsonPath("$.data.orderStatus").value(OrderStatus.CREATED.name()))
                        .andExpect(jsonPath("$.data.deliveryStatus").value(DeliveryStatus.WAITING.name()))
                        .andExpect(jsonPath("$.count").value(1))
                        .andDo(print());

                //then
                BDDMockito.then(orderQueryService).should().findOrder(orderNumber1);
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

        Order order;

        @BeforeEach
        void beforeEach() {
            Member member = Member.builder()
                    .name("UserA")
                    .loginId("id_A")
                    .password("00000000")
                    .city("Seoul")
                    .street("Gangnam")
                    .zipcode("01234")
                    .build();
            Delivery delivery = new Delivery(member);
            Album album = Album.builder()
                    .name("BLACKHOLE")
                    .price(15000)
                    .stockQuantity(10)
                    .artist("IVE")
                    .studio("STARSHIP")
                    .build();
            OrderProduct orderProduct3 = OrderProduct.createOrderProduct(album, 5);
            order = Order.createOrder(member, delivery, List.of(orderProduct3));
        }

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                OrderChangeRequest request = new OrderChangeRequest(Map.of(productNumber2, 5));
                String json = objectMapper.writeValueAsString(request);

                OrderResponse orderResponse = OrderResponse.from(order);

                given(orderService.findOrderWithDeliveryPayment(anyString())).willReturn(order);
                given(orderService.getOrderResponse(any(Order.class))).willReturn(orderResponse);

                //when & then
                mvc.perform(patch("/orders/{orderNumber}", orderNumber1)
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
                    softly.check(() -> BDDMockito.then(orderService).should().changeOrder(orderNumber1, request));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(orderNumber1));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderResponse(order));
                });
            }

            @Test
            void idempotency() throws Exception {
                //given
                OrderChangeRequest request = new OrderChangeRequest(Map.of(productNumber2, 5));
                String json = objectMapper.writeValueAsString(request);

                OrderResponse orderResponse = OrderResponse.from(order);

                given(orderService.findOrderWithDeliveryPayment(anyString())).willReturn(order);
                given(orderService.getOrderResponse(any(Order.class))).willReturn(orderResponse);

                //when & then 첫 번째 요청
                mvc.perform(patch("/orders/{orderNumber}", orderNumber1)
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
                    softly.check(() -> BDDMockito.then(orderService).should().changeOrder(orderNumber1, request));
                    softly.check(() -> BDDMockito.then(orderService).should().findOrderWithDeliveryPayment(orderNumber1));
                    softly.check(() -> BDDMockito.then(orderService).should().getOrderResponse(order));
                });

                //when & then 두 번째 요청
                mvc.perform(patch("/orders/{orderNumber}", orderNumber1)
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
                    softly.check(() -> BDDMockito.then(orderService).should(times(2)).changeOrder(orderNumber1, request));
                    softly.check(() -> BDDMockito.then(orderService).should(times(2)).findOrderWithDeliveryPayment(orderNumber1));
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
                String json = objectMapper.writeValueAsString(request);

                //when & then
                mvc.perform(patch("/orders/{orderNumber}", orderNumber1)
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
                OrderChangeRequest request = new OrderChangeRequest(Map.of("lllIIIll00OO", 3));
                String json = objectMapper.writeValueAsString(request);

                willThrow(new DataNotFoundException("존재하지 않는 상품입니다")).given(orderService).changeOrder(anyString(), any(OrderChangeRequest.class));

                //when & then
                mvc.perform(patch("/orders/{orderNumber}", orderNumber1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                        .andExpect(jsonPath("$.message").value("존재하지 않는 상품입니다"))
                        .andDo(print());

                //then
                thenSoftly(softly -> {
                    softly.check(() -> BDDMockito.then(orderService).should().changeOrder(orderNumber1, request));
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
}