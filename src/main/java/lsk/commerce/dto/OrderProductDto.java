package lsk.commerce.dto;

import lsk.commerce.domain.OrderProduct;

public record OrderProductDto(
        String name,
        int price,
        int quantity,
        int orderPrice
) {
    public static OrderProductDto from(OrderProduct orderProduct) {
        return new OrderProductDto(
                orderProduct.getProduct().getName(),
                orderProduct.getProduct().getPrice(),
                orderProduct.getQuantity(),
                orderProduct.getOrderPrice());
    }
}
