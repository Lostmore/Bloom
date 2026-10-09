package app.bloom.activities.events;

import app.bloom.activities.model.Activity;
import app.bloom.activities.model.ActivityStatus;
import app.bloom.activities.repository.ActivityRepository;
import app.bloom.activities.repository.EventRepository;
import app.bloom.activities.service.ActivityLifecycle;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SafetyEventHandler {
    private static final Set<String> TYPES = Set.of("user.blocked", "user.deleted", "account.blocked", "account.deleted");
    private final JdbcClient jdbc;
    private final ActivityRepository activities;
    private final ActivityLifecycle lifecycle;
    private final EventRepository events;

    public SafetyEventHandler(JdbcClient jdbc, ActivityRepository activities, ActivityLifecycle lifecycle, EventRepository events) {
        this.jdbc = jdbc; this.activities = activities; this.lifecycle = lifecycle; this.events = events;
    }

    @Transactional(timeout = 30)
    public void accept(JsonNode event) {
        String type = event.path("type").asText();
        if (!TYPES.contains(type)) return;
        if (event.path("schemaVersion").asInt() != 1) throw new IllegalArgumentException("Unsupported safety event schema");
        UUID eventId = UUID.fromString(event.path("eventId").asText());
        UUID user = UUID.fromString(event.path("userId").asText());
        if (jdbc.sql("INSERT INTO consumed_events (event_id) VALUES (?) ON CONFLICT DO NOTHING").param(eventId).update() == 0) return;
        UUID other = type.equals("user.blocked") ? UUID.fromString(event.path("data").path("targetId").asText()) : null;
        if (user.equals(other)) throw new IllegalArgumentException("Invalid blocked pair");
        if (other == null) activities.lockAccounts(user); else activities.lockAccounts(user, other);
        for (Activity row : activities.affected(user, other)) {
            Activity activity = lifecycle.refresh(row);
            if (activity.status() == ActivityStatus.FINISHED) continue;
            if (other != null) {
                // The creator stays; otherwise the blocked target leaves the shared meeting.
                UUID removed = activity.creatorId().equals(other) ? user : other;
                lifecycle.remove(activity, removed, "USER_BLOCKED");
            } else if (activity.creatorId().equals(user)) {
                lifecycle.cancel(activity, user, type.endsWith("deleted") ? "ACCOUNT_DELETED" : "ACCOUNT_BLOCKED");
            } else {
                lifecycle.remove(activity, user, type.endsWith("deleted") ? "ACCOUNT_DELETED" : "ACCOUNT_BLOCKED");
            }
        }
        if (type.endsWith("deleted")) {
            activities.markDeleted(user);
            // Remove participant identity from history too, locking rows before changing memberships.
            var history = jdbc.sql("""
                    SELECT a.* FROM activities a WHERE EXISTS
                        (SELECT 1 FROM activity_participants p WHERE p.activity_id = a.id AND p.user_id = ?)
                    ORDER BY a.id FOR UPDATE OF a
                    """).param(user).query(Activity.class).list();
            for (Activity activity : history) {
                if (activities.removeParticipant(activity.id(), user)) {
                    events.append(activities.recount(activity.id()), "activity.participant_left", user, "ACCOUNT_DELETED");
                }
            }
            jdbc.sql("""
                    UPDATE activities SET title = 'Удалённая активность', description = '',
                        approximate_area = '', latitude = 0, longitude = 0, updated_at = now()
                    WHERE creator_id = ?
                    """).param(user).update();
        }
    }
}
