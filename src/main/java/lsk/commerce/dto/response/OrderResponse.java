package lsk.commerce.dto.response;

import lombok.Getter;
import lsk.commerce.dto.OrderProductDto;
import lsk.commerce.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class OrderResponse {

    private List<OrderProductDto> orderProducts = new ArrayList<>();

    private int totalAmount;

    private OrderStatus orderStatus;

    private LocalDateTime orderDate;

    private PaymentStatus paymentStatus;

    private LocalDateTime paymentDate;

    private DeliveryStatus deliveryStatus;

    private LocalDateTime shippedDate;

    private LocalDateTime deliveredDate;

    public OrderResponse(List<OrderProductDto> orderProducts, int totalAmount, OrderStatus orderStatus, LocalDateTime orderDate, PaymentStatus paymentStatus,
                         LocalDateTime paymentDate, DeliveryStatus deliveryStatus, LocalDateTime shippedDate, LocalDateTime deliveredDate) {
        this.orderProducts = orderProducts;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
        this.deliveryStatus = deliveryStatus;
        this.shippedDate = shippedDate;
        this.deliveredDate = deliveredDate;
    }

    public static OrderResponse orderChangeResponse(Order order) {
        List<OrderProductDto> orderProductForms = new ArrayList<>();
        for (OrderProduct orderProduct : order.getOrderProducts()) {
            OrderProductDto orderProductForm = OrderProductDto.orderProductChangeForm(orderProduct);
            orderProductForms.add(orderProductForm);
        }

        if (order.getPayment() == null) {
            return new OrderResponse(orderProductForms, order.getTotalAmount(), order.getOrderStatus(), order.getOrderDate(), null,
                    null, order.getDelivery().getDeliveryStatus(), order.getDelivery().getShippedDate(), order.getDelivery().getDeliveredDate());
        } else {
            return new OrderResponse(orderProductForms, order.getTotalAmount(), order.getOrderStatus(), order.getOrderDate(), order.getPayment().getPaymentStatus(),
                    order.getPayment().getPaymentDate(), order.getDelivery().getDeliveryStatus(), order.getDelivery().getShippedDate(), order.getDelivery().getDeliveredDate());
        }
    }
}
