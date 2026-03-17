package lsk.commerce.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Product;
import lsk.commerce.domain.product.Album;
import lsk.commerce.domain.product.Book;
import lsk.commerce.domain.product.Movie;
import lsk.commerce.dto.request.ProductRequest;
import lsk.commerce.dto.request.ProductUpdateRequest;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.dto.response.ProductWithCategoryResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.query.ProductQueryService;
import lsk.commerce.query.dto.ProductSearchCond;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.CategoryService;
import lsk.commerce.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryProductService categoryProductService;
    private final ProductQueryService productQueryService;

    @PostMapping("/products")
    public ResponseEntity<Result<String>> create(
            @RequestBody @Valid ProductRequest request,
            @RequestParam @Size(min = 1) List<@NotBlank @Size(max = 20) String> categoryNames
    ) {
        productService.register(request, categoryNames);
        return ResponseEntity.ok(new Result<>(request.name(), 1));
    }

    @GetMapping("/products")
    public List<ProductResponse> productList(@ModelAttribute ProductSearchCond cond) {
        return productQueryService.searchProducts(cond);
    }

    @GetMapping("/products/{productName}")
    public ProductResponse findProduct(@PathVariable("productName") String productName) {
        Product product = productService.findProductByName(productName);
        return productService.getProductDto(product);
    }

    @PostMapping("/products/{productName}")
    public ProductResponse updateProduct(@PathVariable("productName") String productName, ProductUpdateRequest request) {
        Product product = productService.updateProduct(productName, request.getPrice(), request.getStockQuantity());
        return productService.getProductDto(product);
    }

    @DeleteMapping("/products/{productName}")
    public String delete(@PathVariable("productName") String productName) {
        productService.deleteProduct(productName);
        return "delete";
    }

    @PostMapping("/products/{productName}/{categoryName}")
    public ProductWithCategoryResponse connectCategory(@PathVariable("productName") String productName, @PathVariable("categoryName") String categoryName) {
        Product product = categoryProductService.connect(productName, categoryName);
        return productService.getProductWithCategoryDto(product);
    }
}
