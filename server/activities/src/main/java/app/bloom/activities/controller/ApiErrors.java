package app.bloom.activities.controller;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ProblemDetail concurrentChange() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "CONCURRENT_CHANGE_RETRY");
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ProblemDetail unavailable() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "ACTIVITIES_UNAVAILABLE");
    }
}
