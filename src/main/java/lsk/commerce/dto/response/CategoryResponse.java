package lsk.commerce.dto.response;

import lombok.Getter;
import lsk.commerce.domain.Category;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CategoryResponse {

    private String name;

    private List<CategoryResponse> child = new ArrayList<>();

    public CategoryResponse(String name, List<CategoryResponse> child) {
        this.name = name;
        this.child = child;
    }

    public static CategoryResponse categoryChangeDto(Category category) {
        List<CategoryResponse> categoryResponses = new ArrayList<>();
        for (Category childCategory : category.getChild()) {
            CategoryResponse categoryResponse = categoryChangeDto(childCategory);
            categoryResponses.add(categoryResponse);
        }

        return new CategoryResponse(category.getName(), categoryResponses);
    }
}
