package lsk.commerce.dto.response;

import lsk.commerce.domain.Order;
import lsk.commerce.dto.OrderProductDto;

import java.util.List;

public record OrderChangeResponse(
        List<OrderProductDto> orderProductDtoList,
        int totalAmount
) {
    public static OrderChangeResponse from(Order order) {
        List<OrderProductDto> orderProductForms = order.getOrderProducts().stream()
                .map(OrderProductDto::from)
                .toList();

        return new OrderChangeResponse(orderProductForms, order.getTotalAmount());
    }
}
