package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.dto.request.ProductRequest;
import lsk.commerce.dto.request.CategoryRequest;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.dto.response.ProductResponse;
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

    @PostMapping("/categories")
    public String create(@Valid CategoryRequest request) {
        if (request.getParentName() != null) {
            if (categoryService.findCategoryByName(request.getParentName()) == null) {
                throw new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + request.getParentName());
            }

            Category parentCategory = categoryService.findCategoryByName(request.getParentName());
            Category childCategory = Category.createChildCategory(parentCategory, request.getName());
            categoryService.create(childCategory);
            return childCategory.getName() + " created";
        }

        Category parentCategory = Category.createParentCategory(request.getName());
        categoryService.create(parentCategory);
        return parentCategory.getName() + " created";
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
        if (categoryService.findCategoryByName(categoryName) == null) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName);
        }

        Category category = categoryService.findCategoryByName(categoryName);
        return categoryService.getCategoryDto(category);
    }

    @PostMapping("/categories/{categoryName}")
    public CategoryResponse changeParentCategory(@PathVariable("categoryName") String categoryName, String newParentCategoryName) {
        if (categoryService.findCategoryByName(categoryName) == null) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName);
        } else if (categoryService.findCategoryByName(newParentCategoryName) == null) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + newParentCategoryName);
        }

        Category category = categoryService.findCategoryByName(categoryName);
        Category newParentCategory = categoryService.findCategoryByName(newParentCategoryName);
        categoryService.changeParentCategory(category, newParentCategory);
        return categoryService.getCategoryDto(category);
    }

    @DeleteMapping("/categories/{categoryName}")
    public String delete(@PathVariable("categoryName") String categoryName) {
        if (categoryService.findCategoryByName(categoryName) == null) {
            throw new IllegalArgumentException("존재하지 않는 카테고리 입니다. name: " + categoryName);
        }

        Category category = categoryService.findCategoryByName(categoryName);
        categoryService.deleteCategory(category);
        return "delete";
    }

    @GetMapping("/categories/{categoryName}/products")
    public List<ProductResponse> findProductsByCategory(@PathVariable("categoryName") String categoryName) {
        if (categoryService.findCategoryByName(categoryName) == null) {
            throw new IllegalArgumentException("존재하지 않는 카테고리 입니다. name: " + categoryName);
        }

        List<Product> products = categoryService.findProductsByCategoryName(categoryName);
        List<ProductResponse> productResponses = new ArrayList<>();

        for (Product product : products) {
            ProductResponse productDto = productService.getProductDto(product);
            productResponses.add(productDto);
        }

        return productResponses;
    }
}
