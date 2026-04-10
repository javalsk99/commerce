package lsk.commerce.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

public record MemberSearchCond(
        @Schema(description = "**이름**은 한글, 초성, 영문, 숫자, _만 사용하여 1~50자 사이로 입력해 주세요.", example = "ㅇㅈ")
        @Pattern(regexp = "^[A-Za-z가-힣ㄱ-ㅎ0-9_]{1,50}", message = "이름은 한글, 초성, 영문, 숫자, _만 사용하여 1~50자 사이로 입력해 주세요")
        String name,
        @Schema(description = "**아이디**는 영문, 숫자, _만 사용하여 1~20자 사이로 입력해 주세요.", example = "id")
        @Pattern(regexp = "^[A-Za-z0-9_]{1,20}$", message = "아이디는 영문, 숫자, _만 사용하여 1~20자 사이로 입력해 주세요")
        String loginId
) {
}
