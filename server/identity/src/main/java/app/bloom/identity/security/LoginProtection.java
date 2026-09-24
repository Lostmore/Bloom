package app.bloom.identity.security;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoginProtection {
    private final JdbcClient jdbc;

    public LoginProtection(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ResponseStatusException.class)
    public void require(String scope, String value, int limit, Duration window, boolean backoff) {
        String key = TokenHasher.sha256(scope + ":" + value);
        jdbc.sql("""
                INSERT INTO auth_attempts (key_hash, attempts, window_start, next_attempt_at)
                VALUES (?, 0, now(), now()) ON CONFLICT DO NOTHING
                """).param(key).update();
        var row = jdbc.sql("SELECT *, now() AS observed_at FROM auth_attempts WHERE key_hash = ? FOR UPDATE")
                .param(key).query((result, index) -> new Attempt(
                        result.getInt("attempts"), result.getTimestamp("window_start").toInstant(),
                        result.getTimestamp("next_attempt_at").toInstant(),
                        result.getTimestamp("observed_at").toInstant())).single();
        Instant now = row.observedAt();
        boolean reset = !row.windowStart().plus(window).isAfter(now);
        int attempts = reset ? 0 : row.attempts();
        if (!reset && (attempts >= limit || row.nextAttempt().isAfter(now))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later");
        }
        int nextCount = attempts + 1;
        long delay = backoff && nextCount >= 3 ? Math.min(60, 1L << Math.min(nextCount - 3, 6)) : 0;
        jdbc.sql("""
                UPDATE auth_attempts
                SET attempts = ?, window_start = ?, next_attempt_at = ?
                WHERE key_hash = ?
                """)
                .params(nextCount, Timestamp.from(reset ? now : row.windowStart()),
                        Timestamp.from(now.plusSeconds(delay)), key)
                .update();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginSucceeded(String phone) {
        jdbc.sql("DELETE FROM auth_attempts WHERE key_hash = ?")
                .param(TokenHasher.sha256("login:" + phone)).update();
    }

    private record Attempt(
            int attempts,
            Instant windowStart,
            Instant nextAttempt,
            Instant observedAt
    ) {
    }
}
