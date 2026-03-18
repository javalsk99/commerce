package lsk.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lsk.commerce.config.WebConfig;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.exception.DataNotFoundException;
import lsk.commerce.query.OrderQueryService;
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

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

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
    String orderNumber = "dfbu398xueos";

    @Nested
    class Create {

        @Nested
        class SuccessCase {

            @Test
            void basic() throws Exception {
                //given
                OrderCreateRequest request = new OrderCreateRequest("id_A", Map.of(productNumber1, 3, productNumber2, 2));
                String json = objectMapper.writeValueAsString(request);

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

                return Stream.of(
                        argumentSet("memberLoginId null", new OrderCreateRequest(null, Map.of(productNumber, 3))),
                        argumentSet("productMap null", new OrderCreateRequest("id_A", null)),
                        argumentSet("productMap empty", new OrderCreateRequest("id_A", Collections.emptyMap())),
                        argumentSet("productMap key 12자 미만", new OrderCreateRequest("id_A", Map.of("l".repeat(11), 3))),
                        argumentSet("productMap key 12자 초과", new OrderCreateRequest("id_A", Map.of("l".repeat(13), 3))),
                        argumentSet("productMap value 100 초과", new OrderCreateRequest("id_A", Map.of(productNumber, 101))),
                        argumentSet("memberLoginId 공백", new OrderCreateRequest(" ", Map.of(productNumber, 3))),
                        argumentSet("memberLoginId 4자 미만", new OrderCreateRequest("a".repeat(3), Map.of(productNumber, 3))),
                        argumentSet("memberLoginId 20자 초과", new OrderCreateRequest("a".repeat(21), Map.of(productNumber, 3)))
                );
            }
        }
    }
}