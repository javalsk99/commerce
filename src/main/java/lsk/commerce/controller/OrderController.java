package lsk.commerce.controller;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Order;
import lsk.commerce.dto.response.OrderResponse;
import lsk.commerce.service.OrderService;
import lsk.commerce.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping("/orders")
    public String create(String memberLoginId, @RequestBody Map<String, Integer> productMap) {
        if (productMap.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다.");
        }

        Long orderId = orderService.order(memberLoginId, productMap);

        return orderId + " created";
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse findOrder(@PathVariable("orderId") Long orderId) {
        Order order = orderService.findOrder(orderId);
        return orderService.getOrderResponse(order);
    }

    @PostMapping("/orders/{orderId}/payments")
    public OrderResponse requestPayment(@PathVariable("orderId") Long orderId) {
        Order order = orderService.findOrder(orderId);
        paymentService.request(order);
        return orderService.getOrderResponse(order);
    }
}
