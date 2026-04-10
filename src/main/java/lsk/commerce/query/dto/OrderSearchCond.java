package lsk.commerce.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lsk.commerce.domain.DeliveryStatus;
import lsk.commerce.domain.OrderStatus;
import lsk.commerce.domain.PaymentStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Builder
public record OrderSearchCond(
        @Schema(
                description = "**회원 아이디**는 영문, 숫자, _만 사용하여 4~20자 사이로 입력해 주세요. \n\n" +
                        "**회원 아이디**는 일치해야 합니다.",
                example = "testId"
        )
        @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "회원 아이디는 영문, 숫자, _만 사용하여 4~20자 사이로 입력해 주세요")
        String memberLoginId,

        @Schema(description = "**상품 이름**은 한글, 초성, 영문, 숫자, 공백, 특수문자(!#&+,.:_-)만 사용하여 1~50자 사이로 입력해 주세요.", example = "ㅍㄹㄱㄹㅁ")
        @Pattern(regexp = "^[A-Za-z가-힣ㄱ-ㅎ0-9 !#&+,.:_-]{1,50}$", message = "상품 이름은 한글, 초성, 영문, 숫자, 공백, 특수문자(!#&+,.:_-)만 사용하여 1~50자 사이로 입력해 주세요")
        String productName,

        @Schema(
                description = "**주문 상태** 검색 조건입니다. \n\n" +
                        "검색 조건으로 사용하지 않을 경우 --로 선택해 주세요.",
                example = "CREATED"
        )
        OrderStatus orderStatus,
        @Schema(description = "**검색 시작일** -을 포함한 yyyy-MM-dd 형식으로 입력해 주세요. (해당 날짜 00:00:00부터 포함)", example = "2026-04-04")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @Schema(description = "**검색 종료일** -을 포함한 yyyy-MM-dd 형식으로 입력해 주세요. (다음 날짜 00:00:00 직전까지 포함)", example = "2026-04-04")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        @Schema(description = "**결제 상태** 검색 조건입니다.")
        PaymentStatus paymentStatus,

        @Schema(description = "**배송 상태** 검색 조건입니다.")
        DeliveryStatus deliveryStatus
) {
}
