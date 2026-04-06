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
                        @ExampleObject(name = "이름 누락", value = "{\"code\": \"NOT_VALID\", \"message\": \"이름은 필수입니다\"}"),
                        @ExampleObject(name = "이름 길이 오류", value = "{\"code\": \"NOT_VALID\", \"message\": \"이름은 2자에서 50자 사이로 입력해 주세요\"}"),
                        @ExampleObject(name = "아이디 누락", value = "{\"code\": \"NOT_VALID\", \"message\": \"아이디는 필수입니다\"}"),
                        @ExampleObject(name = "아이디 길이 오류", value = "{\"code\": \"NOT_VALID\", \"message\": \"아이디는 4자에서 20자 사이로 입력해 주세요\"}"),
                        @ExampleObject(name = "비밀번호 누락", value = "{\"code\": \"NOT_VALID\", \"message\": \"비밀번호는 필수입니다\"}"),
                        @ExampleObject(name = "비밀번호 길이 오류", value = "{\"code\": \"NOT_VALID\", \"message\": \"비밀번호는 8자에서 20자 사이로 입력해 주세요\"}"),
                        @ExampleObject(name = "도시명 누락", value = "{\"code\": \"NOT_VALID\", \"message\": \"도시명은 필수입니다\"}"),
                        @ExampleObject(name = "도시명 길이 오류", value = "{\"code\": \"NOT_VALID\", \"message\": \"도시명은 50자 이하로 입력해 주세요\"}"),
                        @ExampleObject(name = "거리명 누락", value = "{\"code\": \"NOT_VALID\", \"message\": \"거리명은 필수입니다\"}"),
                        @ExampleObject(name = "거리명 길이 오류", value = "{\"code\": \"NOT_VALID\", \"message\": \"거리명은 50자 이하로 입력해 주세요\"}"),
                        @ExampleObject(name = "우편번호 누락", value = "{\"code\": \"NOT_VALID\", \"message\": \"우편번호는 필수입니다\"}"),
                        @ExampleObject(name = "우편번호 길이 오류", value = "{\"code\": \"NOT_VALID\", \"message\": \"우편번호는 50자 이하로 입력해 주세요\"}"),
                        @ExampleObject(name = "아이디 중복", value = "{\"code\": \"BAD_ARGUMENT\", \"message\": \"이미 사용 중인 아이디입니다\"}")
                }
        )
)
public @interface ApiValidationMember {
}
