package lsk.commerce.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import lsk.commerce.domain.OrderStatus;

@QueryProjection
public record OrderSearchResponse(
        String orderNumber,
        int totalAmount,
        OrderStatus orderStatus
) {
}
