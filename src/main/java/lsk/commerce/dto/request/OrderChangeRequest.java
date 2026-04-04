package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderChangeRequest(
        @Schema(example = "[{\"productNumber\": \"WxgG3CzGZhAZ\", \"quantity\": 5}, {\"productNumber\": \"9fyd3T9RxFPZ\", \"quantity\": 2}]")
        @Valid @NotEmpty List<OrderProductRequest> orderProductRequestList
) {
}
