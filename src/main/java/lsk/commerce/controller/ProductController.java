package lsk.commerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import lsk.commerce.exception.ErrorResult;
import lsk.commerce.query.ProductQueryService;
import lsk.commerce.query.dto.ProductSearchCond;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.ProductService;
import lsk.commerce.swagger.ApiRoleError;
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
                    "**상품 이름**: 필수, 50자 이하 \n\n" +
                    "**가격**: 필수, 100원 이상 \n\n" +
                    "**재고**: 필수, 0개 이상 \n\n" +
                    "**dtype**필드에 (필수) 앨범은 **A**, 책은 **B**, 영화는 **M**을 적어주세요. \n\n" +
                    "**A**타입 선택 시 author, isbn, actor, director를 지워주세요. \n\n" +
                    "**artist**: 50자 이하 \n\n" +
                    "**studio**: 50자 이하 \n\n" +
                    "**B**타입 선택 시 artist, studio, actor, director를 지워주세요. \n\n" +
                    "**author**: 50자 이하 \n\n" +
                    "**isbn**: 10자 이상, 13자 이하 \n\n" +
                    "**M**타입 선택 시 artist, studio, author, isbn을 지워주세요. \n\n" +
                    "**actor**: 50자 이하 \n\n" +
                    "**director**: 50자 이하 \n\n" +
                    "**상품 이름**, **artist**, **studio**의 조합은 유일해야 합니다. \n\n" +
                    "**상품 이름**, **author**, **isbn**의 조합은 유일해야 합니다. \n\n" +
                    "**상품 이름**, **actor**, **director**의 조합은 유일해야 합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = ErrorResult.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 카테고리", content = @Content(schema = @Schema(implementation = ErrorResult.class)))
    })
    @ApiRoleError
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "비 로그인", content = @Content(schema = @Schema(implementation = ErrorResult.class)))
    })
    @GetMapping("/products")
    public ResponseEntity<Result<List<ProductResponse>>> productList(@ParameterObject @ModelAttribute ProductSearchCond cond) {
        List<ProductResponse> productResponseList = productQueryService.searchProducts(cond);
        return ResponseEntity.ok(new Result<>(productResponseList, productResponseList.size()));
    }

    @Operation(summary = "상품 상세 조회", description = "상품의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "401", description = "비 로그인", content = @Content(schema = @Schema(implementation = ErrorResult.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품", content = @Content(schema = @Schema(implementation = ErrorResult.class)))
    })
    @GetMapping("/products/{productNumber}")
    public ResponseEntity<Result<ProductDetailResponse>> findProduct(
            @Parameter(description = "**12**자리의 상품 번호를 입력해 주세요.", example = "WxgG3CzGZhAZ")
            @PathVariable("productNumber") String productNumber
    ) {
        Product product = productService.findProduct(productNumber);
        ProductDetailResponse productResponse = productService.getProductDto(product);
        return ResponseEntity.ok(new Result<>(productResponse, 1));
    }

    @Operation(
            summary = "상품 수정",
            description = "**관리자**만 수정할 수 있습니다. \n\n" +
                    "수정할 **가격** (100원 이상) 또는 **재고**를 (0개 이상) 입력해 주세요."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품", content = @Content(schema = @Schema(implementation = ErrorResult.class)))
    })
    @ApiRoleError
    @PatchMapping("/products/{productNumber}")
    public ResponseEntity<Result<ProductDetailResponse>> changeProduct(
            @Parameter(description = "**12**자리의 상품 번호를 입력해 주세요.", example = "WxgG3CzGZhAZ")
            @PathVariable("productNumber") String productNumber,
            @RequestBody @Valid ProductChangeRequest request
    ) {
        Product product = productService.changePriceAndStock(productNumber, request);
        ProductDetailResponse productResponse = productService.getProductDto(product);
        return ResponseEntity.ok(new Result<>(productResponse, 1));
    }

    @Operation(
            summary = "상품 삭제",
            description = "**관리자**만 삭제할 수 있습니다. \n\n" +
                    "예시 상품은 삭제되지 않고 성공합니다."
    )
    @ApiResponse(responseCode = "200")
    @ApiRoleError
    @DeleteMapping("/products/{productNumber}")
    public ResponseEntity<Result<String>> delete(
            @Parameter(description = "**12**자리의 상품 번호를 입력해 주세요.", example = "WxgG3CzGZhAZ")
            @PathVariable("productNumber") String productNumber
    ) {
        productService.deleteProduct(productNumber);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }

    @Operation(summary = "카테고리와 연결", description = "**관리자**만 연결할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 카테고리 또는 상품", content = @Content(schema = @Schema(implementation = ErrorResult.class)))
    })
    @ApiRoleError
    @PatchMapping("/products/{productNumber}/{categoryName}")
    public ResponseEntity<Result<ProductNameWithCategoryNameResponse>> connectCategory(
            @Parameter(description = "**12**자리의 상품 번호를 입력해 주세요.", example = "WxgG3CzGZhAZ")
            @PathVariable("productNumber") String productNumber,
            @Parameter(example = "Java")
            @PathVariable("categoryName") String categoryName
    ) {
        Product product = categoryProductService.connect(productNumber, categoryName);
        ProductNameWithCategoryNameResponse productWithCategoryResponse = productService.getProductWithCategoryDto(product);
        return ResponseEntity.ok(new Result<>(productWithCategoryResponse, 1));
    }
}
