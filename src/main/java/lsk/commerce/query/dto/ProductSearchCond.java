package lsk.commerce.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record ProductSearchCond(
        @Schema(
                description = "**카테고리 이름**은 한글, 영문, 숫자, 공백, _만 사용하여 1~20자 사이로 입력해 주세요. \n\n" +
                        "**카테고리 이름**으로 검색 시 **일치**하는 이름으로 적어주세요.",
                example = "웹 개발"
        )
        @Pattern(regexp = "^[A-Za-z가-힣0-9 _]{1,20}$", message = "카테고리 이름은 한글, 영문, 숫자, 공백, _만 사용하여 1~20자 사이로 입력해 주세요")
        String categoryName,

        @Schema(description = "**상품 이름**은 한글, 초성, 영문, 숫자, 공백, 특수문자(!#&+,.:_-)만 사용하여 1~50자 사이로 입력해 주세요.", example = "ㅍㄹㄱㄹㅁ")
        @Pattern(regexp = "^[A-Za-z가-힣ㄱ-ㅎ0-9 !#&+,.:_-]{1,50}$", message = "상품 이름은 한글, 초성, 영문, 숫자, 공백, 특수문자(!#&+,.:_-)만 사용하여 1~50자 사이로 입력해 주세요")
        String productName,
        @Schema(
                description = "**최소 가격**은 100원 이상이어야 합니다. \n\n" +
                        "**최소 가격**은 최대 가격보다 큰 값 입력 시 검색 결과가 반환되지 않습니다.",
                example = "10000"
        )
        @Min(value = 100, message = "최소 가격은 100원 이상이어야 합니다")
        Integer minPrice,
        @Schema(
                description = "**최대 가격**은 100원 이상이어야 합니다. \n\n" +
                        "**최대 가격**은 최소 가격보다 작은 값 입력 시 검색 결과가 반환되지 않습니다.",
                example = "15000"
        )
        @Min(value = 100, message = "최대 가격은 100원 이상이어야 합니다")
        Integer maxPrice,

        @Schema(description = "**가수**는 한글, 초성, 영문, 숫자, 공백, 특수문자((),._)만 사용하여 1~50자 사이로 입력해 주세요.")
        @Pattern(regexp = "^[A-Za-z가-힣ㄱ-ㅎ0-9 (),._]{1,50}$", message = "가수는 한글, 초성, 영문, 숫자, 공백, 특수문자((),._)만 사용하여 1~50자 사이로 입력해 주세요")
        String artist,
        @Schema(description = "**기획사**는 한글, 초성, 영문, 숫자, 공백만 사용하여 1~50자 사이로 입력해 주세요.")
        @Pattern(regexp = "^[A-Za-z가-힣ㄱ-ㅎ0-9 ]{1,50}$", message = "기획사는 한글, 초성, 영문, 숫자, 공백만 사용하여 1~50자 사이로 입력해 주세요")
        String studio,

        @Schema(description = "**작가**는 한글, 초성, 영문, 공백, 특수문자((),)만 사용하여 1~50자 사이로 입력해 주세요.")
        @Pattern(regexp = "^[A-Za-z가-힣ㄱ-ㅎ (),]{1,50}$", message = "작가는 한글, 초성, 영문, 공백, 특수문자((),)만 사용하여 1~50자 사이로 입력해 주세요")
        String author,
        @Schema(description = "**isbn**은 숫자만 사용하여 1~13자로 입력해 주세요.")
        @Pattern(regexp = "^\\d{1,13}$", message = "isbn은 숫자만 사용하여 1~13자로 입력해 주세요")
        String isbn,

        @Schema(description = "**배우**는 한글, 초성, 영문, 공백, (,)만 사용하여 1~50자 사이로 입력해 주세요.")
        @Pattern(regexp = "^[A-Za-z가-힣ㄱ-ㅎ ,]{1,50}$", message = "배우는 한글, 초성, 영문, 공백, (,)만 사용하여 1~50자 사이로 입력해 주세요")
        String actor,
        @Schema(description = "**감독**은 한글, 초성, 영문만 사용하여 1~50자 사이로 입력해 주세요.")
        @Pattern(regexp = "^[A-Za-z가-힣ㄱ-ㅎ]{1,50}$", message = "감독은 한글, 초성, 영문만 사용하여 1~50자 사이로 입력해 주세요")
        String director
) {
}
