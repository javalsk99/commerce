package lsk.commerce.controller.form;

import lombok.Getter;
import lsk.commerce.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class OrderForm {

    private Long id;
    private String memberLoginId;
    private int totalAmount;
    private LocalDateTime orderDate;
    private List<OrderProductForm> orderProducts;
    private String paymentId;
    private OrderStatus orderStatus;
    private DeliveryStatus deliveryStatus;
    private PaymentStatus paymentStatus;

    public OrderForm(Long id, String memberLoginId, int totalAmount, LocalDateTime orderDate, List<OrderProductForm> orderProducts, String paymentId, OrderStatus orderStatus, DeliveryStatus deliveryStatus, PaymentStatus paymentStatus) {
        this.id = id;
        this.memberLoginId = memberLoginId;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.orderProducts = orderProducts;
        this.paymentId = paymentId;
        this.orderStatus = orderStatus;
        this.deliveryStatus = deliveryStatus;
        this.paymentStatus = paymentStatus;
    }

    public static OrderForm orderChangeForm(Order order) {
        List<OrderProductForm> orderProductForms = new ArrayList<>();
        for (OrderProduct orderProduct : order.getOrderProducts()) {
            OrderProductForm orderProductForm = OrderProductForm.orderProductChangeForm(orderProduct);
            orderProductForms.add(orderProductForm);
        }

        if (order.getPayment() == null) {
            return new OrderForm(order.getId(), order.getMember().getLoginId(), order.getTotalAmount(), order.getOrderDate(), orderProductForms, null, order.getOrderStatus(), order.getDelivery().getDeliveryStatus(), null);
        }
        return new OrderForm(order.getId(), order.getMember().getLoginId(), order.getTotalAmount(), order.getOrderDate(), orderProductForms, order.getPayment().getPaymentId(), order.getOrderStatus(), order.getDelivery().getDeliveryStatus(), order.getPayment().getPaymentStatus());
    }
}
