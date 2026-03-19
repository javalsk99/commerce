package lsk.commerce.query.dto;

import lombok.Builder;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;

import java.time.LocalDate;

@Builder
public record OrderSearchCond(
        String memberLoginId,

        String productName,

        OrderStatus orderStatus,
        LocalDate startDate,
        LocalDate endDate,

        PaymentStatus paymentStatus,

        DeliveryStatus deliveryStatus
) {
}
