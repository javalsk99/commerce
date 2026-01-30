package lsk.commerce.dto.request;

import lombok.Getter;
import lsk.commerce.dto.OrderProductDto;
import lsk.commerce.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class OrderRequest {

    private String orderNumber;
    private String memberLoginId;
    private int totalAmount;
    private LocalDateTime orderDate;
    private List<OrderProductDto> orderProducts;
    private String paymentId;
    private OrderStatus orderStatus;
    private DeliveryStatus deliveryStatus;
    private PaymentStatus paymentStatus;

    public OrderRequest(String orderNumber, String memberLoginId, int totalAmount, LocalDateTime orderDate, List<OrderProductDto> orderProducts, String paymentId, OrderStatus orderStatus, DeliveryStatus deliveryStatus, PaymentStatus paymentStatus) {
        this.orderNumber = orderNumber;
        this.memberLoginId = memberLoginId;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.orderProducts = orderProducts;
        this.paymentId = paymentId;
        this.orderStatus = orderStatus;
        this.deliveryStatus = deliveryStatus;
        this.paymentStatus = paymentStatus;
    }

    public static OrderRequest orderChangeRequest(Order order) {
        List<OrderProductDto> orderProductForms = new ArrayList<>();
        for (OrderProduct orderProduct : order.getOrderProducts()) {
            OrderProductDto orderProductForm = OrderProductDto.orderProductChangeForm(orderProduct);
            orderProductForms.add(orderProductForm);
        }

        if (order.getPayment() == null) {
            return new OrderRequest(order.getOrderNumber(), order.getMember().getLoginId(), order.getTotalAmount(), order.getOrderDate(), orderProductForms, null, order.getOrderStatus(), order.getDelivery().getDeliveryStatus(), null);
        }
        return new OrderRequest(order.getOrderNumber(), order.getMember().getLoginId(), order.getTotalAmount(), order.getOrderDate(), orderProductForms, order.getPayment().getPaymentId(), order.getOrderStatus(), order.getDelivery().getDeliveryStatus(), order.getPayment().getPaymentStatus());
    }
}
