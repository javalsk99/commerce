package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderCreateRequest(
        @Schema(example = "[{\"productNumber\": \"WxgG3CzGZhAZ\", \"quantity\": 3}, {\"productNumber\": \"9fyd3T9RxFPZ\", \"quantity\": 4}]")
        @Valid @NotEmpty List<OrderProductRequest> orderProductRequestList
) {
}
