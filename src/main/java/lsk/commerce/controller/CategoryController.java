package lsk.commerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lsk.commerce.domain.Category;
import lsk.commerce.dto.request.CategoryChangeParentRequest;
import lsk.commerce.dto.request.CategoryCreateRequest;
import lsk.commerce.dto.response.CategoryDisconnectResponse;
import lsk.commerce.dto.response.CategoryResponse;
import lsk.commerce.dto.response.Result;
import lsk.commerce.query.CategoryQueryService;
import lsk.commerce.query.dto.CategoryQueryDto;
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

@Tag(name = "03. 카테고리", description = "생성, 조회, 수정, 삭제, 상품과 연결 해제")
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryProductService categoryProductService;
    private final CategoryQueryService categoryQueryService;

    @Operation(
            summary = "카테고리 생성",
            description = "**관리자**만 생성할 수 있습니다. \n\n" +
                    "**이름**은 중복될 수 없습니다. \n\n" +
                    "**부모 카테고리 이름**은 존재하는 이름이어야 합니다. \n\n" +
                    "**최상위 카테고리**를 만들 때는 **부모 카테고리 이름**을 지워주세요."
    )
    @PostMapping("/categories")
    public ResponseEntity<Result<String>> create(@RequestBody @Valid CategoryCreateRequest request) {
        String categoryName = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Result<>(categoryName, 1));
    }

    @Operation(summary = "카테고리 목록 조회", description = "전체 최상위 카테고리와 하위 카테고리 목록을 조회합니다.")
    @GetMapping("/categories")
    public ResponseEntity<Result<List<CategoryResponse>>> categoryList() {
        List<Category> categories = categoryService.findCategories();
        List<CategoryResponse> categoryResponseList = categories.stream()
                .map(categoryService::getCategoryDto)
                .toList();
        return ResponseEntity.ok(new Result<>(categoryResponseList, categoryResponseList.size()));
    }

    @Operation(summary = "카테고리 상세 조회", description = "카테고리와 연결된 상품 목록을 조회합니다.")
    @GetMapping("/categories/{categoryName}")
    public ResponseEntity<Result<CategoryQueryDto>> findCategory(
            @Parameter(example = "가요")
            @PathVariable("categoryName") String categoryName
    ) {
        CategoryQueryDto categoryQueryDto = categoryQueryService.findCategory(categoryName);
        return ResponseEntity.ok(new Result<>(categoryQueryDto, 1));
    }

    @Operation(
            summary = "부모 카테고리 변경",
            description = "**관리자**만 변경할 수 있습니다. \n\n" +
                    "**자식 카테고리**를 부모 카테고리로 변경할 수 없습니다."
    )
    @PatchMapping("/categories/{categoryName}")
    public ResponseEntity<Result<CategoryResponse>> changeParentCategory(
            @Parameter(example = "가요_001")
            @PathVariable("categoryName") String categoryName,
            @RequestBody @Valid CategoryChangeParentRequest request) {
        Category category = categoryService.changeParentCategory(categoryName, request);
        CategoryResponse categoryResponse = categoryService.getCategoryDto(category);
        return ResponseEntity.ok(new Result<>(categoryResponse, 1));
    }

    @Operation(
            summary = "카테고리 삭제",
            description = "**관리자**만 삭제할 수 있습니다. \n\n" +
                    "**자식 카테고리**가 있으면 삭제할 수 없습니다. \n\n" +
                    "**상품**과 연결되어 있으면 삭제할 수 없습니다."
    )
    @DeleteMapping("/categories/{categoryName}")
    public ResponseEntity<Result<String>> delete(
            @Parameter(example = "가요_001")
            @PathVariable("categoryName") String categoryName
    ) {
        categoryService.deleteCategory(categoryName);
        return ResponseEntity.ok(new Result<>("delete", 1));
    }

    @Operation(
            summary = "상품과 연결 해제",
            description = "**관리자**만 연결 해제할 수 있습니다. \n\n" +
                    "**카테고리 상세 조회**를 통해 연결된 상품이 있는 지 확인하고 **상품 번호**를 입력해 주세요."
    )
    @DeleteMapping("/categories/{categoryName}/{productNumber}")
    public ResponseEntity<Result<CategoryDisconnectResponse>> disconnectProduct(
            @Parameter(example = "가요_001")
            @PathVariable("categoryName") String categoryName,
            @Parameter(description = "**12**자리의 상품 번호를 입력해 주세요.", example = "9fyd3T9RxFPZ")
            @PathVariable("productNumber") String productNumber) {
        Category category = categoryProductService.disconnect(categoryName, productNumber);
        CategoryDisconnectResponse categoryDisconnectResponse = categoryService.getCategoryDisconnectResponse(category);
        return ResponseEntity.ok(new Result<>(categoryDisconnectResponse, 1));
    }

    @Operation(summary = "모든 상품과 연결 해제", description = "**관리자**만 연결 해제할 수 있습니다.")
    @DeleteMapping("/categories/{categoryName}/products")
    public ResponseEntity<Result<CategoryDisconnectResponse>> disconnectProducts(
            @Parameter(example = "가요_001")
            @PathVariable("categoryName") String categoryName
    ) {
        Category category = categoryProductService.disconnectAll(categoryName);
        CategoryDisconnectResponse categoryDisconnectResponse = categoryService.getCategoryDisconnectResponse(category);
        return ResponseEntity.ok(new Result<>(categoryDisconnectResponse, 1));
    }
}
