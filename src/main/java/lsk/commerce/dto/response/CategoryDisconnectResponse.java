package lsk.commerce.dto.response;

import lsk.commerce.domain.Category;

import java.util.List;

public record CategoryDisconnectResponse(
        String name,
        List<ProductResponse> productResponseList
) {

    public static CategoryDisconnectResponse from(Category category) {
        return new CategoryDisconnectResponse(
                category.getName(),
                category.getCategoryProducts().stream()
                        .map(categoryProduct -> ProductResponse.productChangeDto(categoryProduct.getProduct()))
                        .toList()
        );
    }
}
