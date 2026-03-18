package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Order;
import lsk.commerce.dto.request.OrderCreateRequest;
import lsk.commerce.dto.response.OrderResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.query.OrderQueryService;
import lsk.commerce.query.dto.OrderQueryDto;
import lsk.commerce.query.dto.OrderSearchCond;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final OrderQueryService orderQueryService;

    @PostMapping("/orders")
    public ResponseEntity<Result<String>> create(@RequestBody @Valid OrderCreateRequest request) {
        String orderNumber = orderService.order(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Result<>(orderNumber, 1));
    }

    @GetMapping("/orders")
    public List<OrderQueryDto> orderList(@ModelAttribute OrderSearchCond cond) {
        return orderQueryService.searchOrders(cond);
    }

    @GetMapping("/orders/{orderNumber}")
    public OrderQueryDto findOrder(@PathVariable("orderNumber") String orderNumber) {
        return orderQueryService.findOrder(orderNumber);
    }

    @PostMapping("/orders/{orderNumber}")
    public OrderResponse changeOrder(@PathVariable("orderNumber") String orderNumber, @RequestBody Map<String, Integer> newProductMap) {
        if (newProductMap.isEmpty()) {
            throw new IllegalArgumentException("주문을 수정할 주문 상품이 없습니다");
        }

        orderService.updateOrder(orderNumber, newProductMap);
        Order order = orderService.findOrderWithDeliveryPayment(orderNumber);
        return orderService.getOrderResponse(order);
    }

    @DeleteMapping("/orders/{orderNumber}")
    public String delete(@PathVariable("orderNumber") String orderNumber) {
        orderService.deleteOrder(orderNumber);
        return "delete";
    }

    @PostMapping("/orders/{orderNumber}/payments")
    public OrderResponse requestPayment(@PathVariable("orderNumber") String orderNumber) {
        Order order = paymentService.request(orderNumber);
        return orderService.getOrderResponse(order);
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    public OrderResponse cancelOrder(@PathVariable("orderNumber") String orderNumber) {
        Order canceledOrder = orderService.cancelOrder(orderNumber);
        return orderService.getOrderResponse(canceledOrder);
    }
}
