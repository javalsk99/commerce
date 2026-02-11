package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.CategoryProduct;
import lsk.commerce.dto.request.CategoryRequest;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.response.CategoryDisconnectResponse;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.query.dto.ProductSearchCond;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final CategoryProductService categoryProductService;

    @PostMapping("/categories")
    public String create(@Valid CategoryRequest request) {
        String categoryName = categoryService.create(request.getName(), request.getParentName());
        return categoryName + " created";
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categoryList() {
        List<Category> categories = categoryService.findCategories();
        List<CategoryResponse> categoryResponses = new ArrayList<>();

        for (Category category : categories) {
            CategoryResponse categoryDto = categoryService.getCategoryDto(category);
            categoryResponses.add(categoryDto);
        }

        return categoryResponses;
    }

    @GetMapping("/categories/{categoryName}")
    public CategoryResponse findCategory(@PathVariable("categoryName") String categoryName) {
        Category category = categoryService.findCategoryByName(categoryName);
        return categoryService.getCategoryDto(category);
    }

    @PostMapping("/categories/{categoryName}")
    public CategoryResponse changeParentCategory(@PathVariable("categoryName") String categoryName, String newParentCategoryName) {
        Category category = categoryService.changeParentCategory(categoryName, newParentCategoryName);
        return categoryService.getCategoryDto(category);
    }

    @DeleteMapping("/categories/{categoryName}")
    public String delete(@PathVariable("categoryName") String categoryName) {
        categoryService.deleteCategory(categoryName);
        return "delete";
    }

    @PostMapping("/categories/{categoryName}/{productName}")
    public CategoryDisconnectResponse disconnectProduct(@PathVariable("categoryName") String categoryName, @PathVariable("productName") String productName) {
        Category category = categoryProductService.disconnect(categoryName, productName);
        return categoryService.getCategoryDisconnectResponse(category);
    }

    @PostMapping("/categories/{categoryName}/products")
    public CategoryDisconnectResponse disconnectProducts(@PathVariable("categoryName") String categoryName) {
        Category category = categoryProductService.disconnectAll(categoryName);
        return categoryService.getCategoryDisconnectResponse(category);
    }
}
