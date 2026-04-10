package lsk.commerce.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lsk.commerce.exception.ErrorResult;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ApiResponse(
        responseCode = "400",
        content = @Content(
                schema = @Schema(implementation = ErrorResult.class),
                examples = {
                        @ExampleObject(name = "비밀번호 null", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"field\": \"password\", \"message\": \"비밀번호는 필수입니다\"}]}"),
                        @ExampleObject(name = "비밀번호 빈 문자열 (공백 포함)", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"field\": \"password\", \"message\": \"비밀번호는 필수입니다\"}, {\"field\": \"password\", \"message\": \"비밀번호는 영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요\"}]}"),
                        @ExampleObject(name = "비밀번호 패턴 불일치", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"field\": \"password\", \"message\": \"비밀번호는 영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요\"}]}"),
                        @ExampleObject(name = "아이디 null, 우편번호 패턴 불일치", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"field\": \"loginId\", \"message\": \"아이디는 필수입니다\"}, {\"field\": \"zipcode\", \"message\": \"우편번호는 숫자 5자로 입력해 주세요\"}]}"),
                        @ExampleObject(name = "이름 패턴 불일치, 비밀번호 빈 문자열, 기본 주소 null", value = "{\"code\": \"NOT_VALID\", \"message\": \"입력값이 잘못되었습니다\", \"errors\": [{\"field\": \"name\", \"message\": \"이름은 한글, 영문, 숫자, _만 사용하여 2~50자 사이로 입력해 주세요\"}, {\"field\": \"password\", \"message\": \"비밀번호는 필수입니다\"}, {\"field\": \"password\", \"message\": \"비밀번호는 영문, 숫자, 특수문자(!@#$%^&*()_+=-) 조합으로 8~20자 사이로 입력해 주세요\"}, {\"field\": \"baseAddress\", \"message\": \"기본 주소는 필수입니다\"}]}")

                }
        )
)
public @interface ApiValidationMemberResponse {
}
