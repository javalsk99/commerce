package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.api.portone.CompletePaymentRequest;
import lsk.commerce.domain.Order;
import lsk.commerce.dto.request.OrderChangeRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
    public ResponseEntity<Result<List<OrderQueryDto>>> orderList(@ModelAttribute OrderSearchCond cond) {
        List<OrderQueryDto> orderQueryDtoList = orderQueryService.searchOrders(cond);
        return ResponseEntity.ok(new Result<>(orderQueryDtoList, orderQueryDtoList.size()));
    }

    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<Result<OrderQueryDto>> findOrder(@PathVariable("orderNumber") String orderNumber) {
        OrderQueryDto orderQueryDto = orderQueryService.findOrder(orderNumber);
        return ResponseEntity.ok(new Result<>(orderQueryDto, 1));
    }

    @PatchMapping("/orders/{orderNumber}")
    public ResponseEntity<Result<OrderResponse>> changeOrder(@PathVariable("orderNumber") String orderNumber, @RequestBody @Valid OrderChangeRequest request) {
        orderService.changeOrder(orderNumber, request);
        Order order = orderService.findOrderWithAllExceptMember(orderNumber);
        OrderResponse orderResponse = orderService.getOrderResponse(order);
        return ResponseEntity.ok(new Result<>(orderResponse, 1));
    }

    @DeleteMapping("/orders/{orderNumber}")
    public ResponseEntity<Result<String>> delete(@PathVariable("orderNumber") String orderNumber) {
        orderService.deleteOrder(orderNumber);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }

    @PatchMapping("/orders/{orderNumber}/cancel")
    public ResponseEntity<Result<OrderResponse>> cancelOrder(@PathVariable("orderNumber") String orderNumber) {
        Order canceledOrder = orderService.cancelOrder(orderNumber);
        OrderResponse orderResponse = orderService.getOrderResponse(canceledOrder);
        return ResponseEntity.ok(new Result<>(orderResponse, 1));
    }
}
