package lsk.commerce.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResult> illegalArgumentExHandle(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResult("BAD_ARGUMENT", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResult> illegalStatusExHandle(IllegalStateException e) {
        return ResponseEntity.badRequest().body(new ErrorResult("BAD_STATUS", e.getMessage()));
    }
}
