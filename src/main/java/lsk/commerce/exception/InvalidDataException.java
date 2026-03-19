package lsk.commerce.exception;

/**
 * 서버의 문제로 데이터가 잘못 저장되었을 때, 발생하는 예외이다.
 */
public class InvalidDataException extends RuntimeException {
    public InvalidDataException(String message) {
        super(message);
    }
}
