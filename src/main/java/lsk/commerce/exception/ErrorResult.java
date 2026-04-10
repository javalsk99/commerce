package lsk.commerce.exception;

import java.util.List;
import java.util.Map;

public record ErrorResult(
        String code,
        String message,
        List<Map<String, String>> errors
) {
}
