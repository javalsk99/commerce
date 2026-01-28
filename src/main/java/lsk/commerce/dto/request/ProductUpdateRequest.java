package lsk.commerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ProductUpdateRequest {

    @NotNull
    private Integer price;

    @NotNull
    private Integer stockQuantity;

    public ProductUpdateRequest(Integer price, Integer stockQuantity) {
        this.price = price;
        this.stockQuantity = stockQuantity;
    }
}
