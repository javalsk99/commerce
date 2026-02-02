package lsk.commerce.query.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class OrderProductQueryDto {

    @JsonIgnore
    private String orderNumber;
    private String name;
    private int price;
    private int count;
    private int orderPrice;

    public OrderProductQueryDto(String orderNumber, String name, int price, int count, int totalPrice) {
        this.orderNumber = orderNumber;
        this.name = name;
        this.price = price;
        this.count = count;
        this.orderPrice = totalPrice;
    }
}
