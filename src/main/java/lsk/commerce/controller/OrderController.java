package lsk.commerce.controller;

import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.PaymentStatus;
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

        String orderNumber = orderService.order(memberLoginId, productMap);

        return orderNumber + " created";
    }

    @GetMapping("/orders/{orderNumber}")
    public OrderResponse findOrder(@PathVariable("orderNumber") String orderNumber) {
        Order order = orderService.findOrderByOrderNumber(orderNumber);
        return orderService.getOrderResponse(order);
    }

    @PostMapping("/orders/{orderNumber}")
    public OrderResponse changeOrder(@PathVariable("orderNumber") String orderNumber, @RequestBody Map<String, Integer> productMap) {
        if (productMap.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다.");
        }

        Order order = orderService.findOrderByOrderNumber(orderNumber);
        if (order.getPayment() != null) {
            if (order.getPayment().getPaymentStatus() == PaymentStatus.COMPLETED) {
                throw new IllegalStateException("결제가 완료돼서 수정할 수 없습니다.");
            }
        }

        orderService.updateOrder(orderNumber, productMap);
        return orderService.getOrderResponse(order);
    }

    @DeleteMapping("/orders/{orderNumber}")
    public String delete(@PathVariable("orderNumber") String orderNumber) {
        Order order = orderService.findOrderByOrderNumber(orderNumber);
        orderService.deleteOrder(order);
        return "delete";
    }

    @PostMapping("/orders/{orderNumber}/payments")
    public OrderResponse requestPayment(@PathVariable("orderNumber") String orderNumber) {
        Order order = orderService.findOrderByOrderNumber(orderNumber);
        paymentService.request(order);
        return orderService.getOrderResponse(order);
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    public OrderResponse cancelOrder(@PathVariable("orderNumber") String orderNumber) {
        Order order = orderService.findOrderByOrderNumber(orderNumber);
        orderService.cancelOrder(order);
        return orderService.getOrderResponse(order);
    }
}
