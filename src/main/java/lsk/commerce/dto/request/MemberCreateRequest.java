package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record MemberCreateRequest(
        @Schema(example = "유저")
        @NotBlank(message = "이름은 필수입니다")
        @Pattern(regexp = "^[A-Za-z가-힣0-9_]{2,50}", message = "이름은 한글, 영문, 숫자, _만 사용하여 2~50자 사이로 입력해 주세요")
        String name,
        @Schema(example = "test_id_001")
        @NotBlank(message = "아이디는 필수입니다")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "아이디는 영문, 숫자, _만 사용하여 4~20자 사이로 입력해 주세요")
        String loginId,
        @Schema(example = "abAB12!@")
        @NotBlank(message = "비밀번호는 필수입니다")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#$%^&*()_+=-])[A-Za-z0-9!@#$%^&*()_+=-]{8,20}$", message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요")
        String password,
        @Schema(example = "01234")
        @NotBlank(message = "우편번호는 필수입니다")
        @Pattern(regexp = "^\\d{5}$", message = "우편번호는 숫자 5자로 입력해 주세요")
        String zipcode,
        @Schema(example = "서울시 강남구")
        @NotBlank(message = "기본 주소는 필수입니다")
        @Pattern(regexp = "^[A-Za-z가-힣0-9 -]{1,50}$", message = "기본 주소는 한글, 영문, 숫자, -, 공백만 사용하여 1~50자 사이로 입력해 주세요")
        String baseAddress,
        @Schema(example = "101동 101호")
        @NotBlank(message = "상세 주소는 필수입니다")
        @Pattern(regexp = "^[A-Za-z가-힣0-9 ().,-]{1,100}$", message = "상세 주소는 한글, 영문, 숫자, 특수문자(().,-), 공백만 사용하여 1~100자 사이로 입력해 주세요")
        String detailAddress
        ) {
}
