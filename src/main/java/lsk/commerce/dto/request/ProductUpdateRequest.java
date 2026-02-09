package lsk.commerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class ProductUpdateRequest {

    @NotNull @Min(100)
    private Integer price;

    @NotNull @Min(0)
    private Integer stockQuantity;

    public ProductUpdateRequest(Integer price, Integer stockQuantity) {
        this.price = price;
        this.stockQuantity = stockQuantity;
    }
}
