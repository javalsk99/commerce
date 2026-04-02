package lsk.commerce.dto.response;

import com.querydsl.core.annotations.QueryProjection;

public record ProductResponse(
        String name,
        String productNumber,
        Integer price,
        Integer stockQuantity
) {
    @QueryProjection
    public ProductResponse {
    }
}
