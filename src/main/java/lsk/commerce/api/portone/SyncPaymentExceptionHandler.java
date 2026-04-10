package lsk.commerce.api.portone;

import lombok.extern.slf4j.Slf4j;
import lsk.commerce.exception.ErrorResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public final class SyncPaymentExceptionHandler {

    @ExceptionHandler(SyncPaymentException.class)
    public ResponseEntity<ErrorResult> handleSyncFailure(SyncPaymentException e) {
        log.error("[포트원 오류 발생]: {}", e.getMessage(), e);

        return ResponseEntity.badRequest().body(new ErrorResult("PORTONE_ERROR", "결제 처리 중 오류가 발생했습니다. 잠시만 기다려 주세요", null));
    }
}
