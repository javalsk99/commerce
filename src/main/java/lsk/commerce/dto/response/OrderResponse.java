package lsk.commerce.dto.response;

import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.dto.OrderProductDto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        List<OrderProductDto> orderProductDtoList,
        int totalAmount,
        OrderStatus orderStatus,
        LocalDateTime orderDate,

        PaymentStatus paymentStatus,
        LocalDateTime paymentDate,

        DeliveryStatus deliveryStatus,
        LocalDateTime shippedDate,
        LocalDateTime deliveredDate
) {
    public static OrderResponse from(Order order) {
        List<OrderProductDto> orderProductForms = order.getOrderProducts().stream()
                .map(OrderProductDto::from)
                .toList();

        if (order.getPayment() == null) {
            return new OrderResponse(
                    orderProductForms, order.getTotalAmount(), order.getOrderStatus(), order.getOrderDate(),
                    null, null,
                    order.getDelivery().getDeliveryStatus(), order.getDelivery().getShippedDate(), order.getDelivery().getDeliveredDate()
            );
        }

        return new OrderResponse(
                orderProductForms, order.getTotalAmount(), order.getOrderStatus(), order.getOrderDate(),
                order.getPayment().getPaymentStatus(), order.getPayment().getPaymentDate(),
                order.getDelivery().getDeliveryStatus(), order.getDelivery().getShippedDate(), order.getDelivery().getDeliveredDate()
        );
    }
}
