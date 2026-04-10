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
        responseCode = "401",
        content = @Content(
                schema = @Schema(implementation = ErrorResult.class),
                examples = @ExampleObject(name = "비 로그인", value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"로그인을 해야 접근할 수 있습니다\", \"errors\": null}")
        )
)
public @interface ApiUnauthorizedResponse {
}
