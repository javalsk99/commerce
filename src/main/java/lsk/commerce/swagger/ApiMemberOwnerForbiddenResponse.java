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
@ApiUnauthorizedResponse
@ApiResponse(
        responseCode = "403",
        content = @Content(
                schema = @Schema(implementation = ErrorResult.class),
                examples = @ExampleObject(name = "권한 없음", value = "{\"code\": \"NOT_RESOURCE_OWNER\", \"message\": \"아이디의 주인이 아닙니다\"}")
        )
)
public @interface ApiMemberOwnerForbiddenResponse {
}
