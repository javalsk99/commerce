package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.controller.form.CategoryForm;
import lsk.commerce.controller.form.ProductForm;
import lsk.commerce.domain.Category;
import lsk.commerce.domain.Product;
import lsk.commerce.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/categories")
    public String create(@Valid CategoryForm form) {
        if (form.getParentName() != null) {
            if (categoryService.findCategoryByName(form.getParentName()) == null) {
                throw new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + form.getParentName());
            }

            Category parentCategory = categoryService.findCategoryByName(form.getParentName());
            Category childCategory = Category.createChildCategory(parentCategory, form.getName());
            categoryService.create(childCategory);
            return childCategory.getName() + " created";
        }

        Category parentCategory = Category.createParentCategory(form.getName());
        categoryService.create(parentCategory);
        return parentCategory.getName() + " created";
    }

    @GetMapping("/categories")
    public List<CategoryForm> categoryList() {
        List<Category> categories = categoryService.findCategories();
        List<CategoryForm> categoryForms = new ArrayList<>();

        for (Category category : categories) {
            CategoryForm categoryForm = CategoryForm.categoryChangeForm(category);
            categoryForms.add(categoryForm);
        }

        return categoryForms;
    }

    @GetMapping("/categories/{categoryName}")
    public CategoryForm findCategory(@PathVariable("categoryName") String categoryName) {
        if (categoryService.findCategoryByName(categoryName) == null) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName);
        }

        Category category = categoryService.findCategoryByName(categoryName);
        return CategoryForm.categoryChangeForm(category);
    }

    @PostMapping("/categories/{categoryName}")
    public CategoryForm changeParentCategory(@PathVariable("categoryName") String categoryName, CategoryForm form) {
        if (categoryService.findCategoryByName(categoryName) == null) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + categoryName);
        } else if (categoryService.findCategoryByName(form.getParentName()) == null) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. name: " + form.getParentName());
        }

        Category category = categoryService.findCategoryByName(categoryName);
        Category newParentCategory = categoryService.findCategoryByName(form.getParentName());
        categoryService.changeParentCategory(category, newParentCategory);
        return CategoryForm.categoryChangeForm(newParentCategory);
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
    public List<ProductForm> findProductsByCategory(@PathVariable("categoryName") String categoryName) {
        if (categoryService.findCategoryByName(categoryName) == null) {
            throw new IllegalArgumentException("존재하지 않는 카테고리 입니다. name: " + categoryName);
        }

        List<Product> products = categoryService.findProductsByCategoryName(categoryName);
        List<ProductForm> productForms = new ArrayList<>();

        for (Product product : products) {
            ProductForm productForm = ProductForm.productChangeForm(product);
            productForms.add(productForm);
        }

        return productForms;
    }
}
