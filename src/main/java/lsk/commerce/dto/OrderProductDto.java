package lsk.commerce.dto;

import lombok.Getter;
import lsk.commerce.domain.OrderProduct;

@Getter
public class OrderProductDto {

    private String name;
    private int price;
    private int count;
    private int totalPrice;

    public OrderProductDto(String name, int price, int count, int totalPrice) {
        this.name = name;
        this.price = price;
        this.count = count;
        this.totalPrice = totalPrice;
    }

    public static OrderProductDto orderProductChangeForm(OrderProduct orderProduct) {
        return new OrderProductDto(orderProduct.getProduct().getName(), orderProduct.getProduct().getPrice(), orderProduct.getCount(), orderProduct.getOrderPrice());
    }
}
