package lsk.commerce.dto.response;

import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.Payment;
import lsk.commerce.domain.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        int totalAmount,
        PaymentStatus paymentStatus,

        OrderStatus orderStatus,
        LocalDateTime orderDate,

        DeliveryStatus deliveryStatus
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentAmount(),
                payment.getPaymentStatus(),

                payment.getOrder().getOrderStatus(),
                payment.getOrder().getOrderDate(),

                payment.getOrder().getDelivery().getDeliveryStatus()
        );
    }
}
