package lsk.commerce.query.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record OrderProductQueryDto(
        @JsonIgnore
        String orderNumber,
        String name,
        int price,
        int count,
        int orderPrice
) {
}
