package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MemberChangeAddressRequest(
        @Schema(example = "01235")
        @NotBlank
        @Pattern(regexp = "^\\d{5}$", message = "우편번호는 숫자 5자로 입력해 주세요")
        String zipcode,
        @Schema(example = "서울시 강북구")
        @NotBlank
        @Pattern(regexp = "^[A-Za-z가-힣0-9 -]{1,50}$", message = "기본 주소는 한글, 영문, 숫자, -, 공백만 사용하여 1~50자 사이로 입력해 주세요")
        String baseAddress,
        @Schema(example = "101동 102호")
        @NotBlank
        @Pattern(regexp = "^[A-Za-z가-힣0-9 ().,-]{1,100}$", message = "상세 주소는 한글, 영문, 숫자, 특수문자(().,-), 공백만 사용하여 1~100자 사이로 입력해 주세요")
        String detailAddress
) {
}
