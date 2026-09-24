package app.bloom.identity.controller;

import app.bloom.identity.exception.AuthenticationException;
import app.bloom.identity.exception.InvalidTokenException;
import app.bloom.identity.exception.RegistrationException;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({AuthenticationException.class, InvalidTokenException.class})
    public ResponseEntity<?> authentication(RuntimeException exception) {
        return error(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }

    @ExceptionHandler({RegistrationException.class, DataIntegrityViolationException.class})
    public ResponseEntity<?> registration(RuntimeException exception) {
        return error(HttpStatus.BAD_REQUEST, "Request cannot be completed");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> invalid(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("status", exception.getStatusCode().value(), "message", "Request rejected"));
    }

    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message));
    }
}
