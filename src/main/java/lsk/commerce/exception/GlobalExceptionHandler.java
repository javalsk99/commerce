package lsk.commerce.exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResult> validExHandle(MethodArgumentNotValidException e) {
        String errorMessage = "입력값이 잘못됐습니다";
        if (e.getBindingResult().getFieldError() != null) {
            errorMessage = e.getBindingResult().getFieldError()
                    .getDefaultMessage();
        }

        return ResponseEntity.badRequest().body(new ErrorResult("NOT_VALID", errorMessage));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResult> illegalArgumentExHandle(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResult("BAD_ARGUMENT", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResult> illegalStatusExHandle(IllegalStateException e) {
        return ResponseEntity.badRequest().body(new ErrorResult("BAD_STATUS", e.getMessage()));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResult> jwtExHandle(JwtException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResult("UNAUTHORIZED", e.getMessage()));
    }

    @ExceptionHandler(NotResourceOwnerException.class)
    public ResponseEntity<ErrorResult> notResourceExHandle(NotResourceOwnerException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResult("NOT_RESOURCE_OWNER", e.getMessage()));
    }

    @ExceptionHandler(NotAdminException.class)
    public ResponseEntity<ErrorResult> notAdminExHandle(NotAdminException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResult("NOT_ADMIN", e.getMessage()));
    }

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ErrorResult> dataNotFoundExHandle(DataNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResult("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResult> duplicateResourceExHandle(DuplicateResourceException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResult("DUPLICATE_RESOURCE", e.getMessage()));
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorResult> invalidDataExHandle(InvalidDataException e) {
        log.error("[500 INVALID_DATA] 데이터 정합성 오류 발생: {}", e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResult("INVALID_DATA", "서버에 문제가 생겼습니다. 잠시만 기다려 주세요"));
    }
}
