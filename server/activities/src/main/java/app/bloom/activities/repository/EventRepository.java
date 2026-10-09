package app.bloom.activities.repository;

import app.bloom.activities.model.Activity;
import app.bloom.activities.model.PendingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class EventRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public EventRepository(JdbcClient jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

    public void append(Activity activity, String type, UUID actor, String reason) {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("creatorId", activity.creatorId());
        data.put("status", activity.status());
        data.put("participantCount", activity.participantCount());
        data.put("maxParticipants", activity.maxParticipants());
        data.put("startsAt", activity.startsAt());
        data.put("endsAt", activity.endsAt());
        if (actor != null) data.put("userId", actor);
        if (reason != null) data.put("reason", reason);
        try {
            String payload = json.writeValueAsString(Map.of("eventId", eventId, "schemaVersion", 1,
                    "type", type, "activityId", activity.id(), "version", activity.version(),
                    "occurredAt", Instant.now(), "data", data));
            jdbc.sql("""
                    INSERT INTO outbox_events (id, aggregate_id, event_type, payload)
                    VALUES (?, ?, ?, ?::jsonb)
                    """).params(eventId, activity.id(), type, payload).update();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize activity event", exception);
        }
    }

    public Optional<PendingEvent> lockNext() {
        return jdbc.sql("""
                SELECT sequence, aggregate_id, event_type, payload::text, attempts FROM outbox_events e
                WHERE published_at IS NULL AND next_attempt_at <= now()
                  AND NOT EXISTS (SELECT 1 FROM outbox_events earlier
                    WHERE earlier.aggregate_id = e.aggregate_id AND earlier.sequence < e.sequence AND earlier.published_at IS NULL)
                ORDER BY sequence LIMIT 1 FOR UPDATE SKIP LOCKED
                """).query(PendingEvent.class).optional();
    }

    public void published(long sequence) {
        jdbc.sql("UPDATE outbox_events SET published_at = now() WHERE sequence = ?").param(sequence).update();
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
        jdbc.sql("DELETE FROM nearby_limits WHERE next_request_at < now() - interval '1 day'").update();
    }
}
