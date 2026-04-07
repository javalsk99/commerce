package lsk.commerce.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

public record MemberSearchCond(
        @Schema(description = "**이름**은 초성으로도 검색할 수 있습니다.", example = "ㅇㅈ")
        @Pattern(regexp = "^[A-Za-z가-힣ㄱ-ㅎ0-9_]{1,50}")
        String name,
        @Schema(example = "id")
        @Pattern(regexp = "^[A-Za-z0-9_]{1,20}$")
        String loginId
) {
}
