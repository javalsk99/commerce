package lsk.commerce.dto.response;

import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;

public record OrderCancelResponse(
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        DeliveryStatus deliveryStatus
) {
    public static OrderCancelResponse from(Order order) {
        if (order.getPayment() == null) {
            return new OrderCancelResponse(order.getOrderStatus(), null, order.getDelivery().getDeliveryStatus());
        }
        return new OrderCancelResponse(order.getOrderStatus(), order.getPayment().getPaymentStatus(), order.getDelivery().getDeliveryStatus());
    }
}
