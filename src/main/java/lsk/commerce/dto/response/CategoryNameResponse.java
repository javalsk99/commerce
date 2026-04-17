package lsk.commerce.dto.response;

import lsk.commerce.domain.CategoryProduct;

public record CategoryNameResponse(String categoryName) {
    public static CategoryNameResponse from(CategoryProduct categoryProduct) {
        return new CategoryNameResponse(categoryProduct.getCategory().getName());
    }
}
