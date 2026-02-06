package lsk.commerce.api.portone;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
public final class SyncPaymentExceptionHandler {
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(SyncPaymentException.class)
    public void handleSyncFailure() {
    }
}
