package lsk.commerce.dto.response;

import lombok.Getter;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.domain.Product;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ProductWithCategoryResponse {

    private String name;
    private List<CategoryNameResponse> categoryNames = new ArrayList<>();

    @Getter
    public static class CategoryNameResponse {
        private String categoryName;

        public CategoryNameResponse(String categoryName) {
            this.categoryName = categoryName;
        }

        public static CategoryNameResponse categoryProductChangeResponse(CategoryProduct categoryProduct) {
            return new CategoryNameResponse(categoryProduct.getCategory().getName());
        }
    }

    public ProductWithCategoryResponse(String name, List<CategoryNameResponse> categoryNames) {
        this.name = name;
        this.categoryNames = categoryNames;
    }

    public static ProductWithCategoryResponse productChangeResponse(Product product) {
        List<CategoryNameResponse> categoryNameResponses = new ArrayList<>();
        for (CategoryProduct categoryProduct : product.getCategoryProducts()) {
            CategoryNameResponse categoryNameResponse = CategoryNameResponse.categoryProductChangeResponse(categoryProduct);
            categoryNameResponses.add(categoryNameResponse);
        }

        return new ProductWithCategoryResponse(product.getName(), categoryNameResponses);
    }
}
