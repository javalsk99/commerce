package lsk.commerce.query.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lsk.commerce.domain.OrderProduct;

@Getter @Setter
public class OrderProductQueryDto {

    @JsonIgnore
    private String orderNumber;
    private String name;
    private int price;
    private int count;
    private int orderPrice;

    public OrderProductQueryDto(String orderNumber, String name, int price, int count, int orderPrice) {
        this.orderNumber = orderNumber;
        this.name = name;
        this.price = price;
        this.count = count;
        this.orderPrice = orderPrice;
    }

    public static OrderProductQueryDto changeQueryDto(OrderProduct orderProduct) {
        return new OrderProductQueryDto(orderProduct.getOrder().getOrderNumber(), orderProduct.getProduct().getName(),
                orderProduct.getProduct().getPrice(), orderProduct.getCount(), orderProduct.getOrderPrice());
    }
}
