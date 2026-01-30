package lsk.commerce.dto.response;

import lombok.Getter;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.CategoryProduct;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CategoryDisconnectResponse {

    private String name;

    private List<ProductResponse> products = new ArrayList<>();

    public CategoryDisconnectResponse(String name, List<ProductResponse> products) {
        this.name = name;
        this.products = products;
    }

    public static CategoryDisconnectResponse categoryChangeDisconnectResponse(Category category) {
        List<ProductResponse> productResponses = new ArrayList<>();
        for (CategoryProduct categoryProduct : category.getCategoryProducts()) {
            ProductResponse productResponse = ProductResponse.productChangeDto(categoryProduct.getProduct());
            productResponses.add(productResponse);
        }

        return new CategoryDisconnectResponse(category.getName(), productResponses);
    }
}
