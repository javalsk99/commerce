package lsk.commerce.dto.response;

public record Result<T>(
        T data,
        int count
) {
}
