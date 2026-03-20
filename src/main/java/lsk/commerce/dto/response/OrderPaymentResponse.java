package lsk.commerce.dto.response;

import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;
import lsk.commerce.dto.OrderProductDto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderPaymentResponse(
        String memberLoginId,

        String orderNumber,
        List<OrderProductDto> orderProductDtoList,
        Integer totalAmount,
        OrderStatus orderStatus,
        LocalDateTime orderDate,

        String paymentId,
        PaymentStatus paymentStatus,

        DeliveryStatus deliveryStatus

) {
    public static OrderPaymentResponse from(Order order) {
        List<OrderProductDto> orderProducts = order.getOrderProducts().stream()
                .map(OrderProductDto::from)
                .toList();

        if (order.getPayment() == null) {
            return new OrderPaymentResponse(order.getMember().getLoginId(), order.getOrderNumber(), orderProducts, order.getTotalAmount(), order.getOrderStatus(), order.getOrderDate(), null, null, order.getDelivery().getDeliveryStatus());
        }
        return new OrderPaymentResponse(order.getMember().getLoginId(), order.getOrderNumber(), orderProducts, order.getTotalAmount(), order.getOrderStatus(), order.getOrderDate(), order.getPayment().getPaymentId(), order.getPayment().getPaymentStatus(), order.getDelivery().getDeliveryStatus());
    }
}
