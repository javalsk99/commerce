package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderCreateRequest(
        @Schema(example = "[{\"productNumber\": \"WxgG3CzGZhAZ\", \"quantity\": 3}, {\"productNumber\": \"9fyd3T9RxFPZ\", \"quantity\": 4}]")
        @Valid @NotEmpty(message = "주문할 상품을 한 종류 이상 입력해 주세요") List<OrderProductRequest> orderProductRequestList
) {
    @Schema(hidden = true)
    @AssertTrue(message = "중복된 상품이 포함되어 있습니다")
    public boolean isProductsUnique() {
        if (orderProductRequestList == null || orderProductRequestList.isEmpty()) {
            return true;
        }

        long distinctCount = orderProductRequestList.stream()
                .map(OrderProductRequest::productNumber)
                .distinct()
                .count();

        return orderProductRequestList.size() == distinctCount;
    }
}
