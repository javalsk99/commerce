package lsk.commerce.dto;

import lsk.commerce.domain.OrderProduct;

public record OrderProductDto(
        String name,
        int price,
        int count,
        int orderPrice
) {
    public static OrderProductDto from(OrderProduct orderProduct) {
        return new OrderProductDto(
                orderProduct.getProduct().getName(),
                orderProduct.getProduct().getPrice(),
                orderProduct.getCount(),
                orderProduct.getOrderPrice());
    }
}
