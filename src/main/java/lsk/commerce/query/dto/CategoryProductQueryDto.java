package lsk.commerce.query.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record CategoryProductQueryDto(
        @JsonIgnore
        String categoryName,
        String productName,
        String productNumber
) {
}
