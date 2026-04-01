package lsk.commerce.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.dto.request.ProductChangeRequest;
import lsk.commerce.dto.response.ProductNameWithCategoryNameResponse;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.query.ProductQueryService;
import lsk.commerce.query.dto.ProductSearchCond;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
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
            @RequestBody @Valid ProductCreateRequest request,
            @RequestParam @NotEmpty List<@NotBlank @Size(max = 20) String> categoryNames
    ) {
        productService.register(request, categoryNames);
        return ResponseEntity.ok(new Result<>(request.name(), 1));
    }

    @GetMapping("/products")
    public ResponseEntity<Result<List<ProductResponse>>> productList(@ModelAttribute ProductSearchCond cond) {
        List<ProductResponse> productResponseList = productQueryService.searchProducts(cond);
        return ResponseEntity.ok(new Result<>(productResponseList, productResponseList.size()));
    }

    @GetMapping("/products/{productNumber}")
    public ResponseEntity<Result<ProductResponse>> findProduct(
            @Parameter(example = "WxgG3CzGZhAZ")
            @PathVariable("productNumber") String productNumber
    ) {
        Product product = productService.findProduct(productNumber);
        ProductResponse productResponse = productService.getProductDto(product);
        return ResponseEntity.ok(new Result<>(productResponse, 1));
    }

    @PatchMapping("/products/{productNumber}")
    public ResponseEntity<Result<ProductResponse>> changeProduct(@PathVariable("productNumber") String productNumber, @RequestBody @Valid ProductChangeRequest request) {
        Product product = productService.changePriceAndStock(productNumber, request);
        ProductResponse productResponse = productService.getProductDto(product);
        return ResponseEntity.ok(new Result<>(productResponse, 1));
    }

    @DeleteMapping("/products/{productNumber}")
    public ResponseEntity<Result<String>> delete(@PathVariable("productNumber") String productNumber) {
        productService.deleteProduct(productNumber);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }

    @PatchMapping("/products/{productNumber}/{categoryName}")
    public ResponseEntity<Result<ProductNameWithCategoryNameResponse>> connectCategory(@PathVariable("productNumber") String productNumber, @PathVariable("categoryName") String categoryName) {
        Product product = categoryProductService.connect(productNumber, categoryName);
        ProductNameWithCategoryNameResponse productWithCategoryResponse = productService.getProductWithCategoryDto(product);
        return ResponseEntity.ok(new Result<>(productWithCategoryResponse, 1));
    }
}
