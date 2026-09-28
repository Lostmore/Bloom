package app.bloom.identity.repository;

import app.bloom.identity.model.Account;
import app.bloom.identity.model.IdentityEvent;
import app.bloom.identity.model.IdentityEventData;
import app.bloom.identity.model.PendingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EventRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public EventRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // The caller holds the account lock; this counter is independent of JWT tokenVersion.
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(Account account, String type, UUID familyId, String reason) {
        long version = jdbc.sql("""
                UPDATE accounts SET event_version = event_version + 1
                WHERE id = :id RETURNING event_version
                """).param("id", account.getId()).query(Long.class).single();
        UUID eventId = UUID.randomUUID();
        var data = new IdentityEventData(account.getStatus(), account.getTokenVersion(), familyId, reason);
        try {
            String payload = json.writeValueAsString(new IdentityEvent(
                    eventId, 1, type, account.getId(), version, Instant.now(), data));
            jdbc.sql("""
                    INSERT INTO outbox_events (id, aggregate_id, event_type, payload)
                    VALUES (:id, :user, :type, :payload::jsonb)
                    """).param("id", eventId).param("user", account.getId())
                    .param("type", type).param("payload", payload).update();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize identity event", exception);
        }
    }

    public Optional<PendingEvent> lockNext() {
        return jdbc.sql("""
                SELECT sequence, aggregate_id, event_type, payload::text, attempts
                FROM outbox_events e
                WHERE published_at IS NULL AND next_attempt_at <= now()
                  AND NOT EXISTS (
                      SELECT 1 FROM outbox_events earlier
                      WHERE earlier.aggregate_id = e.aggregate_id
                        AND earlier.sequence < e.sequence AND earlier.published_at IS NULL
                  )
                ORDER BY sequence LIMIT 1 FOR UPDATE SKIP LOCKED
                """).query(PendingEvent.class).optional();
    }

    public void published(long sequence) {
        jdbc.sql("UPDATE outbox_events SET published_at = now() WHERE sequence = :sequence")
                .param("sequence", sequence).update();
    }

    public void retry(PendingEvent event) {
        long seconds = Math.min(300, 1L << Math.min(event.attempts() + 1, 8));
        jdbc.sql("""
                UPDATE outbox_events SET attempts = attempts + 1, next_attempt_at = now() + :delay * interval '1 second'
                WHERE sequence = :sequence
                """).param("sequence", event.sequence()).param("delay", seconds).update();
    }

    public void cleanup() {
        jdbc.sql("DELETE FROM outbox_events WHERE published_at < now() - interval '7 days'").update();
    }
}
