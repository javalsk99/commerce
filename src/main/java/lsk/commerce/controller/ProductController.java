package lsk.commerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.dto.request.ProductChangeRequest;
import lsk.commerce.dto.response.ProductDetailResponse;
import lsk.commerce.dto.response.ProductNameWithCategoryNameResponse;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.query.ProductQueryService;
import lsk.commerce.query.dto.ProductSearchCond;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.ProductService;
import org.springdoc.core.annotations.ParameterObject;
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

@Tag(name = "04. 상품", description = "생성, 검색, 수정, 삭제, 카테고리와 연결")
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryProductService categoryProductService;
    private final ProductQueryService productQueryService;

    @Operation(
            summary = "상품 생성",
            description = "**관리자**만 생성할 수 있습니다. \n\n" +
                    "**카테고리 이름**은 한 개 이상 넣어주세요. \n\n" +
                    "**dtype**필드에 앨범은 **A**, 책은 **B**, 영화는 **M**을 적어주세요. \n\n" +
                    "**A**타입 선택 시 author, isbn, actor, director를 지워주세요. \n\n" +
                    "**B**타입 선택 시 artist, studio, actor, director를 지워주세요. \n\n" +
                    "**M**타입 선택 시 artist, studio, author, isbn을 지워주세요."
    )
    @PostMapping("/products")
    public ResponseEntity<Result<String>> create(
            @Parameter(example = "가요_001, 가요_002")
            @RequestParam @NotEmpty List<@NotBlank @Size(max = 20) String> categoryNames,
            @RequestBody @Valid ProductCreateRequest request
    ) {
        productService.register(request, categoryNames);
        return ResponseEntity.ok(new Result<>(request.name(), 1));
    }

    @Operation(
            summary = "상품 검색",
            description = "검색 조건에 맞춰 조회합니다. \n\n" +
                    "원하지 않는 검색 조건은 비워주세요."
    )
    @GetMapping("/products")
    public ResponseEntity<Result<List<ProductResponse>>> productList(@ParameterObject @ModelAttribute ProductSearchCond cond) {
        List<ProductResponse> productResponseList = productQueryService.searchProducts(cond);
        return ResponseEntity.ok(new Result<>(productResponseList, productResponseList.size()));
    }

    @Operation(summary = "상품 상세 조회", description = "상품의 상세 정보를 조회합니다.")
    @GetMapping("/products/{productNumber}")
    public ResponseEntity<Result<ProductDetailResponse>> findProduct(
            @Parameter(description = "**12**자리의 상품 번호를 입력해주세요.", example = "WxgG3CzGZhAZ")
            @PathVariable("productNumber") String productNumber
    ) {
        Product product = productService.findProduct(productNumber);
        ProductDetailResponse productResponse = productService.getProductDto(product);
        return ResponseEntity.ok(new Result<>(productResponse, 1));
    }

    @Operation(
            summary = "상품 수정",
            description = "**관리자**만 수정할 수 있습니다. \n\n" +
                    "수정할 **가격** 또는 **재고**를 입력해 주세요."
    )
    @PatchMapping("/products/{productNumber}")
    public ResponseEntity<Result<ProductDetailResponse>> changeProduct(
            @Parameter(description = "**12**자리의 상품 번호를 입력해주세요.", example = "WxgG3CzGZhAZ")
            @PathVariable("productNumber") String productNumber,
            @RequestBody @Valid ProductChangeRequest request
    ) {
        Product product = productService.changePriceAndStock(productNumber, request);
        ProductDetailResponse productResponse = productService.getProductDto(product);
        return ResponseEntity.ok(new Result<>(productResponse, 1));
    }

    @Operation(summary = "상품 삭제", description = "**관리자**만 삭제할 수 있습니다.")
    @DeleteMapping("/products/{productNumber}")
    public ResponseEntity<Result<String>> delete(
            @Parameter(description = "**12**자리의 상품 번호를 입력해주세요.", example = "WxgG3CzGZhAZ")
            @PathVariable("productNumber") String productNumber
    ) {
        productService.deleteProduct(productNumber);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }

    @Operation(summary = "카테고리와 연결", description = "**관리자**만 연결할 수 있습니다.")
    @PatchMapping("/products/{productNumber}/{categoryName}")
    public ResponseEntity<Result<ProductNameWithCategoryNameResponse>> connectCategory(
            @Parameter(description = "**12**자리의 상품 번호를 입력해주세요.", example = "WxgG3CzGZhAZ")
            @PathVariable("productNumber") String productNumber,
            @Parameter(example = "Java")
            @PathVariable("categoryName") String categoryName
    ) {
        Product product = categoryProductService.connect(productNumber, categoryName);
        ProductNameWithCategoryNameResponse productWithCategoryResponse = productService.getProductWithCategoryDto(product);
        return ResponseEntity.ok(new Result<>(productWithCategoryResponse, 1));
    }
}
