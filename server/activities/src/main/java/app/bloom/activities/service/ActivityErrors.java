package app.bloom.activities.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ActivityErrors {
    private ActivityErrors() {}

    public static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "ACTIVITY_UNAVAILABLE");
    }

    public static ResponseStatusException conflict(String code) {
        return new ResponseStatusException(HttpStatus.CONFLICT, code);
    }

    public static ResponseStatusException invalid(String code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code);
    }
}
