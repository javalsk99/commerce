package lsk.commerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Product;
import lsk.commerce.dto.request.ProductChangeRequest;
import lsk.commerce.dto.request.ProductCreateRequest;
import lsk.commerce.dto.response.ProductDetailResponse;
import lsk.commerce.dto.response.ProductNameWithCategoryNameResponse;
import lsk.commerce.dto.response.ProductResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.exception.ErrorResult;
import lsk.commerce.query.ProductQueryService;
import lsk.commerce.query.dto.ProductSearchCond;
import lsk.commerce.service.CategoryProductService;
import lsk.commerce.service.ProductService;
import lsk.commerce.swagger.ApiAdminForbiddenResponse;
import lsk.commerce.swagger.ApiUnauthorizedResponse;
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
                    "**상품 이름**: (필수) 한글, 영문, 숫자, 공백, 특수문자(!#&+,.:_-)만 사용하여 1~50자 사이로 입력해 주세요. \n\n" +
                    "**상품 가격**: (필수) 100원 이상이어야 합니다. \n\n" +
                    "**재고**: (필수) 0개 이상이어야 합니다. \n\n" +
                    "**dtype**필드에 (필수) 앨범은 **A**, 책은 **B**, 영화는 **M**을 입력해 주세요. \n\n" +
                    "**A**타입 선택 시 author, isbn, actor, director를 지워주세요. \n\n" +
                    "**artist**: 한글, 영문, 숫자, 공백, 특수문자((),._)만 사용하여 1~50자 사이로 입력해 주세요. \n\n" +
                    "**studio**: 한글, 영문, 숫자, 공백만 사용하여 1~50자 사이로 입력해 주세요. \n\n" +
                    "**B**타입 선택 시 artist, studio, actor, director를 지워주세요. \n\n" +
                    "**author**: 한글, 영문, 공백, 특수문자((),)만 사용하여 1~50자 사이로 입력해 주세요. \n\n" +
                    "**isbn**: 숫자만 사용하여 10자 또는 13자로 입력해 주세요. \n\n" +
                    "**M**타입 선택 시 artist, studio, author, isbn을 지워주세요. \n\n" +
                    "**actor**: 한글, 영문, 공백, (,)만 사용하여 1~50자 사이로 입력해 주세요. \n\n" +
                    "**director**: 한글, 영문만 사용하여 1~50자 사이로 입력해 주세요. \n\n" +
                    "**상품 이름**, **artist**, **studio**의 조합은 유일해야 합니다. \n\n" +
                    "**상품 이름**, **author**, **isbn**의 조합은 유일해야 합니다. \n\n" +
                    "**상품 이름**, **actor**, **director**의 조합은 유일해야 합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "카테고리 이름 미입력", value = "{\"code\": \"BAD_PARAMETER\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"QUERY\", \"field\": \"categoryNames\", \"message\": \"필수 파라미터가 누락되었습니다\"}]}", description = "'lsk-commerce.shop/products' \n\n 'lsk-commerce.shop/products?'"),
                                    @ExampleObject(name = "카테고리 이름 값 미입력", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"QUERY\", \"field\": \"categoryNames\", \"message\": \"카테고리 이름은 1개 이상 입력해 주세요\"}]}", description = "'lsk-commerce.shop/products?categoryNames' \n\n 'lsk-commerce.shop/products?categoryNames='"),
                                    @ExampleObject(name = "첫 번째 카테고리 이름 공백", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"QUERY\", \"field\": \"categoryNames[0]\", \"message\": \"카테고리 이름은 필수입니다\"}, {\"location\": \"QUERY\",\"field\": \"categoryNames[0]\", \"message\": \"카테고리 이름은 한글, 영문, 숫자, 공백, _만 사용하여 1~20자 사이로 입력해 주세요\"}]}", description = "'lsk-commerce.shop/product?categoryNames= '"),
                                    @ExampleObject(name = "dtype 빈 문자열", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"dtype\", \"message\": \"dtype은 필수입니다\"}, {\"location\": \"BODY\", \"field\": \"dtype\", \"message\": \"dtype은 A, B, M만 사용하여 한 글자로 입력해 주세요\"}, {\"location\": \"BODY\", \"field\": \"validFields\", \"message\": \"dtype이 A면 artist, studio B면 author, isbn M이면 actor, director만 입력해 주세요\"}]}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "존재하지 않는 카테고리", value = "{\"code\": \"NOT_FOUND\", \"message\": \"존재하지 않는 카테고리가 있습니다\", \"errors\": null}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "상품 이름, 자식 필드 중복", value = "{\"code\": \"DUPLICATE_RESOURCE\", \"message\": \"이미 존재하는 상품입니다. name: 음악_001\", \"errors\": null}")
                    )
            )
    })
    @ApiAdminForbiddenResponse
    @PostMapping("/products")
    public ResponseEntity<Result<String>> create(
            @Parameter(
                    example = "가요_001, 가요_002",
                    description = "현재 Swagger UI 라이브러리 버그로 인해 categoryNames를 추가하지 않고 전송하면 브라우저단에서 요청이 차단되는 현상이 있습니다. \n\n" +
                            "categoryNames를 추가하고 지우면 이 문제가 해결되지만 입력에서 막혀 Postman에서 전송해야 아래의 예시처럼 나옵니다."
            )
            @RequestParam @NotEmpty(message = "카테고리 이름은 1개 이상 입력해 주세요") List<@NotBlank(message = "카테고리 이름은 필수입니다") @Pattern(regexp = "^[A-Za-z가-힣0-9 _]{1,20}$", message = "카테고리 이름은 한글, 영문, 숫자, 공백, _만 사용하여 1~20자 사이로 입력해 주세요") String> categoryNames,
            @RequestBody @Valid ProductCreateRequest request
    ) {
        productService.register(request, categoryNames);
        return ResponseEntity.ok(new Result<>(request.name(), 1));
    }

    @Operation(
            summary = "상품 검색",
            description = "검색 조건에 맞춰 조회합니다. \n\n" +
                    "원하지 않는 검색 조건은 비워주세요. \n\n" +
                    "Postman에서 최소 가격과 최대 가격은 숫자를 넣지 않으면 기본 예외 메시지가 출력됩니다. (빈 문자열과 공백은 무시됩니다. '  12  3 ' -> '123') \n\n" +
                    "초성과 한글은 섞이면 검색 결과가 나오지 않습니다. \n\n" +
                    "ex) 프로그래ㅁ (X), jPa ㅍㄹㄱㄹㅁ (O)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "카테고리 이름 빈 문자열 (공백 미 포함, 패턴 불일치 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"FORM\", \"field\": \"categoryName\", \"message\": \"카테고리 이름은 한글, 영문, 숫자, 공백, _만 사용하여 1~20자 사이로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "가수 빈 문자열 (공백 미 포함, 패턴 불일치 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"FORM\", \"field\": \"artist\", \"message\": \"가수는 한글, 초성, 영문, 숫자, 공백, 특수문자((),._)만 사용하여 1~50자 사이로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "최소 가격 100원 미만", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"FORM\", \"field\": \"minPrice\", \"message\": \"최소 가격은 100원 이상이어야 합니다\"}]}"),
                                    @ExampleObject(name = "최소 가격 타입 불일치", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"FORM\", \"field\": \"minPrice\", \"message\": \"Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'; For input string: \\\"a\\\"\"}]}")
                            }
                    )
            )
    })
    @ApiUnauthorizedResponse
    @GetMapping("/products")
    public ResponseEntity<Result<List<ProductResponse>>> productList(@ParameterObject @ModelAttribute @Valid ProductSearchCond cond) {
        List<ProductResponse> productResponseList = productQueryService.searchProducts(cond);
        return ResponseEntity.ok(new Result<>(productResponseList, productResponseList.size()));
    }

    @Operation(summary = "상품 상세 조회", description = "상품의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "상품 번호 공백 (패턴 불일치 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"PATH\", \"field\": \"productNumber\", \"message\": \"상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요\"}]}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "존재하지 않는 상품", value = "{\"code\": \"NOT_FOUND\", \"message\": \"존재하지 않는 상품입니다. productNumber: WxgG3CzGZhAZ\", \"errors\": null}")
                    )
            )
    })
    @ApiUnauthorizedResponse
    @GetMapping("/products/{productNumber}")
    public ResponseEntity<Result<ProductDetailResponse>> findProduct(
            @Parameter(description = "**12**자리의 상품 번호를 입력해 주세요.", example = "WxgG3CzGZhAZ")
            @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
            @PathVariable("productNumber") String productNumber
    ) {
        Product product = productService.findProduct(productNumber);
        ProductDetailResponse productDetailResponse = productService.getProductDto(product);
        return ResponseEntity.ok(new Result<>(productDetailResponse, 1));
    }

    @Operation(
            summary = "상품 수정",
            description = "**관리자**만 수정할 수 있습니다. \n\n" +
                    "수정할 **상품 가격** (100원 이상) 또는 **재고**를 (0개 이상) 입력해 주세요."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "상품 번호 공백 (패턴 불일치 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"PATH\", \"field\": \"productNumber\", \"message\": \"상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "상품 가격, 100원 미만, 재고 0개 미만", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"price\", \"message\": \"상품 가격은 100원 이상이어야 합니다\"}, {\"location\": \"BODY\", \"field\": \"stockQuantity\", \"message\": \"재고는 0개 이상이어야 합니다\"}]}"),
                                    @ExampleObject(name = "상품 가격, 재고 미입력", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"BODY\", \"field\": \"validFieldsPresent\", \"message\": \"상품 가격 또는 재고를 입력해 주세요\"}]}")
                            }
                    )
            ),
            @ApiResponse(responseCode = "404",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "존재하지 않는 상품", value = "{\"code\": \"NOT_FOUND\", \"message\": \"존재하지 않는 상품입니다. productNumber: WxgG3CzGZhAZ\", \"errors\": null}")
                    )
            )
    })
    @ApiAdminForbiddenResponse
    @PatchMapping("/products/{productNumber}")
    public ResponseEntity<Result<ProductDetailResponse>> changeProduct(
            @Parameter(description = "**12**자리의 상품 번호를 입력해 주세요.", example = "WxgG3CzGZhAZ")
            @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = @ExampleObject(name = "상품 번호 공백 (패턴 불일치 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"PATH\", \"field\": \"productNumber\", \"message\": \"상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요\"}]}")
                    )
            )
    })
    @ApiAdminForbiddenResponse
    @DeleteMapping("/products/{productNumber}")
    public ResponseEntity<Result<String>> delete(
            @Parameter(description = "**12**자리의 상품 번호를 입력해 주세요.", example = "WxgG3CzGZhAZ")
            @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
            @PathVariable("productNumber") String productNumber
    ) {
        productService.deleteProduct(productNumber);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }

    @Operation(summary = "카테고리와 연결", description = "**관리자**만 연결할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "상품 번호 공백 (패턴 불일치 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"PATH\", \"field\": \"productNumber\", \"message\": \"상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요\"}]}"),
                                    @ExampleObject(name = "카테고리 이름 공백 (패턴 불일치 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"location\": \"PATH\", \"field\": \"categoryName\", \"message\": \"카테고리 이름은 한글, 영문, 숫자, 공백, _만 사용하여 1~20자 사이로 입력해 주세요\"}]}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResult.class),
                            examples = {
                                    @ExampleObject(name = "존재하지 않는 상품", value = "{\"code\": \"NOT_FOUND\", \"message\": \"존재하지 않는 상품입니다. productNumber: WxgG3CzGZhAZ\", \"errors\": null}"),
                                    @ExampleObject(name = "존재하지 않는 카테고리", value = "{\"code\": \"NOT_FOUND\", \"message\": \"존재하지 않는 카테고리입니다. name: Java\", \"errors\": null}")
                            }
                    )
            )
    })
    @ApiAdminForbiddenResponse
    @PatchMapping("/products/{productNumber}/{categoryName}")
    public ResponseEntity<Result<ProductNameWithCategoryNameResponse>> connectCategory(
            @Parameter(description = "**12**자리의 상품 번호를 입력해 주세요.", example = "WxgG3CzGZhAZ")
            @Pattern(regexp = "^[A-Za-z0-9]{12}$", message = "상품 번호는 영문, 숫자만 사용하여 12자로 입력해 주세요")
            @PathVariable("productNumber") String productNumber,
            @Parameter(example = "Java")
            @Pattern(regexp = "^[A-Za-z가-힣0-9 _]{1,20}$", message = "카테고리 이름은 한글, 영문, 숫자, 공백, _만 사용하여 1~20자 사이로 입력해 주세요")
            @PathVariable("categoryName") String categoryName
    ) {
        Product product = categoryProductService.connect(productNumber, categoryName);
        ProductNameWithCategoryNameResponse productWithCategoryResponse = productService.getProductWithCategoryDto(product);
        return ResponseEntity.ok(new Result<>(productWithCategoryResponse, 1));
    }
}
