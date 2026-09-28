package app.bloom.users.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DeletionRepository {
    private final JdbcClient jdbc;

    public DeletionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void enqueue(UUID id) {
        jdbc.sql("INSERT INTO account_deletions (user_id) VALUES (:id) ON CONFLICT DO NOTHING")
                .param("id", id).update();
    }

    public Optional<UUID> lockNext() {
        return jdbc.sql("""
                SELECT user_id FROM account_deletions
                WHERE completed_at IS NULL AND next_attempt_at <= now()
                ORDER BY requested_at LIMIT 1 FOR UPDATE SKIP LOCKED
                """).query(UUID.class).optional();
    }

    public void complete(UUID id) {
        jdbc.sql("UPDATE account_deletions SET completed_at = now() WHERE user_id = :id")
                .param("id", id).update();
    }

    public void retry(UUID id) {
        jdbc.sql("UPDATE account_deletions SET next_attempt_at = now() + interval '30 seconds' WHERE user_id = :id")
                .param("id", id).update();
    }
}
