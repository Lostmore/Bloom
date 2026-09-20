package app.bloom.identity.exception;

/** Generic auth failure never leaks whether email exists. */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException() {
        super("Invalid email or password");
    }
}
