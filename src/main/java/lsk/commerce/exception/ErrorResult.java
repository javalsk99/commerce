package lsk.commerce.exception;

public record ErrorResult(
        String code,
        String message
) {
}
