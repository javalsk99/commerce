package lsk.commerce.controller;

import lombok.RequiredArgsConstructor;
import lsk.commerce.controller.form.OrderForm;
import lsk.commerce.domain.Order;
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
        Long orderId = orderService.order(memberLoginId, productMap);

        return orderId + " created";
    }

    @GetMapping("/orders/{orderId}")
    public OrderForm orderList(@PathVariable("orderId") Long orderId) {
        Order order = orderService.findOrder(orderId);
        return OrderForm.orderChangeForm(order);
    }

    @PostMapping("/orders/{orderId}/payment")
    public OrderForm requestPayment(@PathVariable("orderId") Long orderId) {
        Order order = orderService.findOrder(orderId);
        paymentService.request(order);
        return OrderForm.orderChangeForm(order);
    }
}
