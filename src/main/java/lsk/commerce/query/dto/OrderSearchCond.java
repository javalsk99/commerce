package lsk.commerce.query.dto;

import lombok.Getter;
import lombok.Setter;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;

import java.time.LocalDate;

@Getter @Setter
public class OrderSearchCond {

    private String memberLoginId;

    private String productName;

    private OrderStatus orderStatus;
    private LocalDate startDate;
    private LocalDate endDate;

    private PaymentStatus paymentStatus;

    private DeliveryStatus deliveryStatus;

    public OrderSearchCond(String memberLoginId, String productName, OrderStatus orderStatus, LocalDate startDate,
                           LocalDate endDate, PaymentStatus paymentStatus, DeliveryStatus deliveryStatus) {
        this.memberLoginId = memberLoginId;
        this.productName = productName;
        this.orderStatus = orderStatus;
        this.startDate = startDate;
        this.endDate = endDate;
        this.paymentStatus = paymentStatus;
        this.deliveryStatus = deliveryStatus;
    }
}
