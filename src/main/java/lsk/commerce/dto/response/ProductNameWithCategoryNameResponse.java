package lsk.commerce.dto.response;

import lsk.commerce.domain.Product;

import java.util.List;

public record ProductNameWithCategoryNameResponse(
        String name,
        List<CategoryNameResponse> categoryNameResponseList
) {
    public static ProductNameWithCategoryNameResponse from(Product product) {
        List<CategoryNameResponse> categoryNameResponses = product.getCategoryProducts().stream()
                .map(CategoryNameResponse::from)
                .toList();

        return new ProductNameWithCategoryNameResponse(product.getName(), categoryNameResponses);
    }
}
