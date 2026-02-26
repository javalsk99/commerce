package lsk.commerce.query.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.annotations.QueryProjection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.Order;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@EqualsAndHashCode(of = "orderNumber")
public class OrderQueryDto {

    @JsonIgnore
    private String loginId;
    private String orderNumber;
    private List<OrderProductQueryDto> orderProducts = new ArrayList<>();
    private int totalAmount;
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDate;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime shippedDate;
    private LocalDateTime deliveredDate;

    @QueryProjection
    public OrderQueryDto(String loginId, String orderNumber, int totalAmount, OrderStatus orderStatus, LocalDateTime orderDate, PaymentStatus paymentStatus,
                         LocalDateTime paymentDate, DeliveryStatus deliveryStatus, LocalDateTime shippedDate, LocalDateTime deliveredDate) {
        this.loginId = loginId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
        this.deliveryStatus = deliveryStatus;
        this.shippedDate = shippedDate;
        this.deliveredDate = deliveredDate;
    }

    public OrderQueryDto(String orderNumber, int totalAmount, OrderStatus orderStatus, LocalDateTime orderDate, PaymentStatus paymentStatus,
                         LocalDateTime paymentDate, DeliveryStatus deliveryStatus, LocalDateTime shippedDate, LocalDateTime deliveredDate) {
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
        this.deliveryStatus = deliveryStatus;
        this.shippedDate = shippedDate;
        this.deliveredDate = deliveredDate;
    }

    public static OrderQueryDto changeQueryDto(Order order) {
        return new OrderQueryDto(order.getMember().getLoginId(), order.getOrderNumber(), order.getTotalAmount(), order.getOrderStatus(), order.getOrderDate(), order.getPayment().getPaymentStatus(),
                order.getPayment().getPaymentDate(), order.getDelivery().getDeliveryStatus(), order.getDelivery().getShippedDate(), order.getDelivery().getDeliveredDate());
    }
}
