package lsk.commerce.exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResult> notReadableExHandle(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErrorResult("BAD_REQUEST", "JSON 형식이 잘못되었습니다. 예시 형식에 맞춰 작성해 주세요", null));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResult> missingParameterExHandle(MissingServletRequestParameterException e) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("location", "QUERY");
        map.put("field", e.getParameterName());
        map.put("message", "필수 파라미터가 누락되었습니다");

        return ResponseEntity.badRequest().body(new ErrorResult("BAD_PARAMETER", "입력값이 잘못되었습니다", List.of(map)));
    }

    //단독으로 있는 @ModelAttribute도 적용된다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResult> validExHandle(MethodArgumentNotValidException e) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    MethodParameter parameter = e.getParameter();
                    String location = "OTHER";
                    if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
                        location = "FORM";
                    } else if (parameter.hasParameterAnnotation(RequestBody.class)) {
                        location = "BODY";
                    }

                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("location", location);
                    map.put("field", error.getField());
                    map.put("message", error.getDefaultMessage());
                    return map;
                })
                .toList();

        return ResponseEntity.badRequest().body(new ErrorResult("NOT_VALID", "입력값이 잘못되었습니다", errors));
    }

    //@PathVariable이 같이 있는 @RequestBody도 적용된다.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResult> handlerValidExHandle(HandlerMethodValidationException e) {
        List<Map<String, String>> errors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> {
                            String location = getLocation(result);
                            String field = getField(result, error);

                            Map<String, String> map = new LinkedHashMap<>();
                            map.put("location", location);
                            map.put("field", field);
                            map.put("message", error.getDefaultMessage());
                            return map;
                        }))
                .toList();

        return ResponseEntity.badRequest().body(new ErrorResult("NOT_VALID", "입력값이 잘못되었습니다", errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResult> illegalArgumentExHandle(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResult("BAD_ARGUMENT", e.getMessage(), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResult> illegalStatusExHandle(IllegalStateException e) {
        return ResponseEntity.badRequest().body(new ErrorResult("BAD_STATUS", e.getMessage(), null));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResult> jwtExHandle(JwtException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResult("UNAUTHORIZED", e.getMessage(), null));
    }

    @ExceptionHandler(NotResourceOwnerException.class)
    public ResponseEntity<ErrorResult> notResourceExHandle(NotResourceOwnerException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResult("NOT_RESOURCE_OWNER", e.getMessage(), null));
    }

    @ExceptionHandler(NotAdminException.class)
    public ResponseEntity<ErrorResult> notAdminExHandle(NotAdminException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResult("NOT_ADMIN", e.getMessage(), null));
    }

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ErrorResult> dataNotFoundExHandle(DataNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResult("NOT_FOUND", e.getMessage(), null));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResult> duplicateResourceExHandle(DuplicateResourceException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResult("DUPLICATE_RESOURCE", e.getMessage(), null));
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorResult> invalidDataExHandle(InvalidDataException e) {
        log.error("[500 INVALID_DATA] 데이터 정합성 오류 발생: {}", e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResult("INVALID_DATA", "서버에 문제가 생겼습니다. 잠시만 기다려 주세요", null));
    }

    private static String getLocation(ParameterValidationResult result) {
        MethodParameter parameter = result.getMethodParameter();
        String location = "OTHER";
        if (parameter.hasParameterAnnotation(PathVariable.class)) {
            location = "PATH";
        } else if (parameter.hasParameterAnnotation(RequestParam.class)) {
            location = "QUERY";
        } else if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
            location = "FORM";
        } else if (parameter.hasParameterAnnotation(RequestBody.class)) {
            location = "BODY";
        }

        return location;
    }

    private static String getField(ParameterValidationResult result, MessageSourceResolvable error) {
        String field;
        if (error instanceof FieldError fieldError) {
            field = fieldError.getField();
        } else if (error instanceof ObjectError objectError) {
            field = objectError.getObjectName();
        } else {
            field = result.getMethodParameter().getParameterName();
            if (result.getContainerIndex() != null) {
                field += "[" + result.getContainerIndex() + "]";
            }
        }
        return field;
    }
}
