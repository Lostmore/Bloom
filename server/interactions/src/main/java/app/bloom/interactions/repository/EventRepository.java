package app.bloom.interactions.repository;

import app.bloom.interactions.model.PendingEvent;
import app.bloom.interactions.model.Match;
import app.bloom.interactions.model.MatchEvent;
import app.bloom.interactions.model.MatchEventData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class EventRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public EventRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void append(Match match, String type, String reason) {
        UUID eventId = UUID.randomUUID();
        try {
            String payload = json.writeValueAsString(new MatchEvent(eventId, 1, type, match.id(), match.version(), Instant.now(),
                    new MatchEventData(match.userA(), match.userB(), match.status(), reason)));
            jdbc.sql("""
                    INSERT INTO outbox_events (id, aggregate_id, event_type, payload)
                    VALUES (:id, :user, :type, :payload::jsonb)
                    """).param("id", eventId).param("user", match.id()).param("type", type).param("payload", payload).update();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize match event", exception);
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
