package lsk.commerce.api.portone;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.*;

/* 예외 로그 확인을 위해 주석 처리
@ControllerAdvice
public final class SyncPaymentExceptionHandler {
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(SyncPaymentException.class)
    public void handleSyncFailure() {
    }
}
*/
