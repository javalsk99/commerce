package lsk.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record ProductCreateRequest(
        @Schema(example = "음악_001")
        @NotBlank(message = "상품 이름은 필수입니다")
        @Pattern(regexp = "^[A-Za-z가-힣0-9 !#&+,.:_-]{1,50}$", message = "상품 이름은 한글, 영문, 숫자, 공백, 특수문자(!#&+,.:_-)만 사용하여 1~50자 사이로 입력해 주세요")
        String name,
        @Schema(example = "15000")
        @NotNull(message = "상품 가격은 필수입니다") @Min(value = 100, message = "상품 가격은 100원 이상이어야 합니다")
        Integer price,
        @Schema(example = "100")
        @NotNull(message = "재고는 필수입니다") @Min(value = 0, message = "재고는 0개 이상이어야 합니다")
        Integer stockQuantity,

        @Schema(example = "A")
        @NotBlank(message = "dtype은 필수입니다")
        @Pattern(regexp = "^[ABM]$", message = "dtype은 A, B, M만 사용하여 한 글자로 입력해 주세요")
        String dtype,

        @Schema(example = "artist")
        @Pattern(regexp = "^[A-Za-z가-힣0-9 (),._]{1,50}$", message = "가수는 한글, 영문, 숫자, 공백, 특수문자((),._)만 사용하여 1~50자 사이로 입력해 주세요")
        String artist,
        @Schema(example = "studio")
        @Pattern(regexp = "^[A-Za-z가-힣0-9 ]{1,50}$", message = "기획사는 한글, 영문, 숫자, 공백만 사용하여 1~50자 사이로 입력해 주세요")
        String studio,

        @Pattern(regexp = "^[A-Za-z가-힣 (),]{1,50}$", message = "작가는 한글, 영문, 공백, 특수문자((),)만 사용하여 1~50자 사이로 입력해 주세요")
        String author,
        @Pattern(regexp = "^(\\d{10}|\\d{13})$", message = "isbn은 숫자만 사용하여 10자 또는 13자로 입력해 주세요")
        String isbn,

        @Pattern(regexp = "^[A-Za-z가-힣 ,]{1,50}$", message = "배우는 한글, 영문, 공백, (,)만 사용하여 1~50자 사이로 입력해 주세요")
        String actor,
        @Pattern(regexp = "^[A-Za-z가-힣]{1,50}$", message = "감독은 한글, 영문만 사용하여 1~50자 사이로 입력해 주세요")
        String director
) {
    @Schema(hidden = true)
    @AssertTrue(message = "dtype이 A면 artist, studio B면 author, isbn M이면 actor, director만 입력해 주세요")
    public boolean isValidFields() {
        if ("A".equals(dtype)) {
            return (artist != null && studio != null) &&
                    (author == null && isbn == null) &&
                    (actor == null && director == null);
        } else if ("B".equals(dtype)) {
            return (artist == null && studio == null) &&
                    (author != null && isbn != null) &&
                    (actor == null && director == null);
        } else if ("M".equals(dtype)) {
            return (artist == null && studio == null) &&
                    (author == null && isbn == null) &&
                    (actor != null && director != null);
        }

        return false;
    }
}
