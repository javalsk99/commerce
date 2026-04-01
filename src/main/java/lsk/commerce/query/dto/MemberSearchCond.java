package lsk.commerce.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberSearchCond(
        @Schema(example = "ㅇㅈ")
        String name,
        @Schema(example = "id")
        String loginId
) {
}
