package lsk.commerce.query.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
public record OrderQueryDto(
        @JsonIgnore
        String loginId,
        String orderNumber,
        List<OrderProductQueryDto> orderProductQueryDtoList,
        int totalAmount,
        OrderStatus orderStatus,
        LocalDateTime orderDate,
        PaymentStatus paymentStatus,
        LocalDateTime paymentDate,
        DeliveryStatus deliveryStatus,
        LocalDateTime shippedDate,
        LocalDateTime deliveredDate
) {
    @QueryProjection
    public OrderQueryDto(String loginId, String orderNumber, int totalAmount, OrderStatus orderStatus, LocalDateTime orderDate, PaymentStatus paymentStatus,
                         LocalDateTime paymentDate, DeliveryStatus deliveryStatus, LocalDateTime shippedDate, LocalDateTime deliveredDate) {
        this(loginId, orderNumber, new ArrayList<>(), totalAmount, orderStatus, orderDate, paymentStatus, paymentDate, deliveryStatus, shippedDate, deliveredDate);
    }

    public OrderQueryDto(String orderNumber, int totalAmount, OrderStatus orderStatus, LocalDateTime orderDate, PaymentStatus paymentStatus,
                         LocalDateTime paymentDate, DeliveryStatus deliveryStatus, LocalDateTime shippedDate, LocalDateTime deliveredDate) {
        this(null, orderNumber, new ArrayList<>(), totalAmount, orderStatus, orderDate, paymentStatus, paymentDate, deliveryStatus, shippedDate, deliveredDate);
    }
}
