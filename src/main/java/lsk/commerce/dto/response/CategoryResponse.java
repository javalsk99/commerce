package lsk.commerce.dto.response;

import lsk.commerce.domain.Category;

import java.util.List;

public record CategoryResponse(
        String name,
        List<CategoryResponse> children
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getName(),
                category.getChildren().stream()
                        .map(CategoryResponse::from)
                        .toList()
        );
    }
}
