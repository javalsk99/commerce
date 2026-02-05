package lsk.commerce.controller;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.dto.response.OrderResponse;
import lsk.commerce.query.OrderQueryService;
import lsk.commerce.query.dto.OrderQueryDto;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final OrderQueryService orderQueryService;

    @PostMapping("/orders")
    public String create(String memberLoginId, @RequestBody Map<String, Integer> productMap) {
        if (productMap.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다.");
        }

        String orderNumber = orderService.order(memberLoginId, productMap);

        return orderNumber + " created";
    }

    @GetMapping("/orders/{orderNumber}")
    public OrderQueryDto findOrder(@PathVariable("orderNumber") String orderNumber) {
        return orderQueryService.findOrder(orderNumber);
    }

    @PostMapping("/orders/{orderNumber}")
    public OrderResponse changeOrder(@PathVariable("orderNumber") String orderNumber, @RequestBody Map<String, Integer> productMap) {
        if (productMap.isEmpty()) {
            throw new IllegalArgumentException("주문을 수정할 주문 상품이 없습니다.");
        }

        Order order = orderService.updateOrder(orderNumber, productMap);
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
