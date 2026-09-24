package app.bloom.identity.exception;

/** Refresh token invalid, expired, or reuse detected. */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
