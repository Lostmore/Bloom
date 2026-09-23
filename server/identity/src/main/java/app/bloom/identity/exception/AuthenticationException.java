package app.bloom.identity.exception;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException() {
        super("Authentication failed");
    }
}
