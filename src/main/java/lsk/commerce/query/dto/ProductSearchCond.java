package lsk.commerce.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ProductSearchCond(
        @Schema(description = "**카테고리 이름**으로 검색 시 일치하는 이름으로 적어주세요.", example = "웹 개발")
        String categoryName,

        @Schema(description = "**상품 이름**은 초성으로도 검색할 수 있습니다.", example = "ㅍㄹㄱㄹㅁ")
        String productName,
        @Schema(description = "**최소 가격**은 최대 가격보다 큰 값 입력 시 검색 결과가 반환되지 않습니다.", example = "10000")
        Integer minPrice,
        @Schema(description = "**최대 가격**은 최소 가격보다 작은 값 입력 시 검색 결과가 반환되지 않습니다.", example = "15000")
        Integer maxPrice,

        @Schema(description = "**가수**는 초성으로도 검색할 수 있습니다.")
        String artist,
        @Schema(description = "**기획사**는 초성으로도 검색할 수 있습니다.")
        String studio,

        @Schema(description = "**작가**는 초성으로도 검색할 수 있습니다.")
        String author,
        @Schema(description = "**isbn**은 숫자로 적어주세요.")
        String isbn,

        @Schema(description = "**배우**는 초성으로도 검색할 수 있습니다.")
        String actor,
        @Schema(description = "**감독**은 초성으로도 검색할 수 있습니다.")
        String director
) {
}
