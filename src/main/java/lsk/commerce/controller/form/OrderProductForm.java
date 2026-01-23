package lsk.commerce.controller.form;

import lombok.Getter;
import lsk.commerce.domain.OrderProduct;

@Getter
public class OrderProductForm {

    private String name;
    private int price;
    private int count;
    private int totalPrice;

    public OrderProductForm(String name, int price, int count, int totalPrice) {
        this.name = name;
        this.price = price;
        this.count = count;
        this.totalPrice = totalPrice;
    }

    public static OrderProductForm orderProductChangeForm(OrderProduct orderProduct) {
        return new OrderProductForm(orderProduct.getProduct().getName(), orderProduct.getProduct().getPrice(), orderProduct.getCount(), orderProduct.getOrderPrice());
    }
}
