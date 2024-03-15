package ua.kiev.splash.mathhelper.exceptions;

public class DuplicateKeyException extends RuntimeException {
    public DuplicateKeyException() {
        super();
    }

    public DuplicateKeyException(String message) {
        super(message);
    }
    public DuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateKeyException(Throwable cause) {
        super(cause);
    }
}