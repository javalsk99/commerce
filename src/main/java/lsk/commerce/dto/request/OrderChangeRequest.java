package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderChangeRequest(
        @Schema(example = "[{\"productNumber\": \"WxgG3CzGZhAZ\", \"quantity\": 5}, {\"productNumber\": \"9fyd3T9RxFPZ\", \"quantity\": 2}]")
        @Valid @NotEmpty List<OrderProductRequest> orderProductRequestList
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
