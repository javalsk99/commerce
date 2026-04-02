package lsk.commerce.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberSearchCond(
        @Schema(description = "**이름**은 초성으로도 검색할 수 있습니다.", example = "ㅇㅈ")
        String name,
        @Schema(example = "id")
        String loginId
) {
}
