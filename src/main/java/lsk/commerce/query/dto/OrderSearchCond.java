package lsk.commerce.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;

import java.time.LocalDate;

@Builder
public record OrderSearchCond(
        @Schema(example = "id")
        String memberLoginId,

        @Schema(description = "**상품 이름**은 초성으로도 검색할 수 있습니다.", example = "ㅍㄹㄱㄹㅁ")
        String productName,

        @Schema(
                description = "**주문 상태** 검색 조건입니다. \n\n" +
                        "검색 조건으로 사용하지 않을 경우 --로 선택해 주세요.",
                example = "CREATED"
        )
        OrderStatus orderStatus,
        @Schema(description = "**검색 시작일** (해당 날짜 00:00:00부터 포함)", example = "2026-04-04")
        LocalDate startDate,
        @Schema(description = "**검색 종료일** (다음 날짜 00:00:00 직전까지 포함)", example = "2026-04-04")
        LocalDate endDate,

        @Schema(description = "**결제 상태** 검색 조건입니다.")
        PaymentStatus paymentStatus,

        @Schema(description = "**배송 상태** 검색 조건입니다.")
        DeliveryStatus deliveryStatus
) {
}
