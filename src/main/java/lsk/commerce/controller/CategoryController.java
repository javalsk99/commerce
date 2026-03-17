package lsk.commerce.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.dto.request.CategoryChangeParentRequest;
import lsk.commerce.dto.request.CategoryRequest;
import lsk.commerce.dto.response.CategoryDisconnectResponse;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryProductService categoryProductService;

    @PostMapping("/categories")
    public ResponseEntity<Result<String>> create(@RequestBody @Valid CategoryRequest request) {
        String categoryName = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Result<>(categoryName, 1));
    }

    @GetMapping("/categories")
    public ResponseEntity<Result<List<CategoryResponse>>> categoryList() {
        List<Category> categories = categoryService.findCategories();
        List<CategoryResponse> categoryResponseList = categories.stream()
                .map(categoryService::getCategoryDto)
                .toList();
        return ResponseEntity.ok(new Result<>(categoryResponseList, categoryResponseList.size()));
    }

    @GetMapping("/categories/{categoryName}")
    public ResponseEntity<Result<CategoryResponse>> findCategory(@PathVariable("categoryName") String categoryName) {
        Category category = categoryService.findCategoryByName(categoryName);
        CategoryResponse categoryResponse = categoryService.getCategoryDto(category);
        return ResponseEntity.ok(new Result<>(categoryResponse, 1));
    }

    @PatchMapping("/categories/{categoryName}")
    public ResponseEntity<Result<CategoryResponse>> changeParentCategory(@PathVariable("categoryName") String categoryName, @RequestBody @Valid CategoryChangeParentRequest request) {
        Category category = categoryService.changeParentCategory(categoryName, request);
        CategoryResponse categoryResponse = categoryService.getCategoryDto(category);
        return ResponseEntity.ok(new Result<>(categoryResponse, 1));
    }

    @DeleteMapping("/categories/{categoryName}")
    public ResponseEntity<Result<String>> delete(@PathVariable("categoryName") String categoryName) {
        categoryService.deleteCategory(categoryName);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }

    @DeleteMapping("/categories/{categoryName}/{productName}")
    public ResponseEntity<Result<CategoryDisconnectResponse>> disconnectProduct(@PathVariable("categoryName") String categoryName, @PathVariable("productName") String productName) {
        Category category = categoryProductService.disconnect(categoryName, productName);
        CategoryDisconnectResponse categoryDisconnectResponse = categoryService.getCategoryDisconnectResponse(category);
        return ResponseEntity.ok(new Result<>(categoryDisconnectResponse, 1));
    }

    @DeleteMapping("/categories/{categoryName}/products")
    public ResponseEntity<Result<CategoryDisconnectResponse>> disconnectProducts(@PathVariable("categoryName") String categoryName) {
        Category category = categoryProductService.disconnectAll(categoryName);
        CategoryDisconnectResponse categoryDisconnectResponse = categoryService.getCategoryDisconnectResponse(category);
        return ResponseEntity.ok(new Result<>(categoryDisconnectResponse, 1));
    }
}
