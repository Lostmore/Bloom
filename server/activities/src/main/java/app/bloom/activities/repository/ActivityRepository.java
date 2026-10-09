package app.bloom.activities.repository;

import app.bloom.activities.dto.ActivityInput;
import app.bloom.activities.model.Activity;
import app.bloom.activities.model.ActivityStatus;
import app.bloom.activities.model.Category;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityRepository {
    private final JdbcClient jdbc;

    public ActivityRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    // Always lock accounts before activities, in the same order as safety event handling.
    public boolean lockAccounts(UUID... users) {
        boolean available = true;
        for (UUID user : Arrays.stream(users).distinct().sorted().toList()) {
            jdbc.sql("INSERT INTO account_guards (user_id) VALUES (?) ON CONFLICT DO NOTHING").param(user).update();
            boolean deleted = jdbc.sql("SELECT deleted FROM account_guards WHERE user_id = ? FOR UPDATE")
                    .param(user).query(Boolean.class).single();
            available &= !deleted;
        }
        return available;
    }

    public void markDeleted(UUID user) {
        jdbc.sql("UPDATE account_guards SET deleted = TRUE WHERE user_id = ?").param(user).update();
    }

    public Optional<Activity> find(UUID id) {
        return jdbc.sql("SELECT * FROM activities WHERE id = ?").param(id).query(Activity.class).optional();
    }

    public Optional<Activity> lock(UUID id) {
        return jdbc.sql("SELECT * FROM activities WHERE id = ? FOR UPDATE").param(id).query(Activity.class).optional();
    }

    public Optional<Activity> byRequest(UUID creator, UUID request) {
        return jdbc.sql("SELECT * FROM activities WHERE creator_id = ? AND request_id = ?")
                .params(creator, request).query(Activity.class).optional();
    }

    public boolean creationLimit(UUID creator) {
        return jdbc.sql("""
                SELECT count(*) >= 20 FROM activities WHERE creator_id = :creator
                  AND (created_at >= now() - interval '1 day'
                    OR (status IN ('OPEN','FULL','STARTED') AND ends_at > now()))
                """).param("creator", creator).query(Boolean.class).single();
    }

    public Activity create(UUID creator, UUID request, String hash, ActivityInput input, Instant ends) {
        Activity result = fields(jdbc.sql("""
                INSERT INTO activities (id, creator_id, request_id, request_hash, title, description, category,
                    approximate_area, latitude, longitude, starts_at, ends_at, max_participants)
                VALUES (:id, :creator, :request, :hash, :title, :description, :category,
                    :area, :latitude, :longitude, :starts, :ends, :maximum) RETURNING *
                """), input, ends).param("id", UUID.randomUUID()).param("creator", creator)
                .param("request", request).param("hash", hash).query(Activity.class).single();
        addParticipant(result.id(), creator);
        return result;
    }

    public Activity edit(Activity activity, ActivityInput input, Instant ends) {
        return fields(jdbc.sql("""
                UPDATE activities SET title = :title, description = :description, category = :category,
                    approximate_area = :area, latitude = :latitude, longitude = :longitude,
                    starts_at = :starts, ends_at = :ends, max_participants = :maximum,
                    status = CASE WHEN participant_count >= :maximum THEN 'FULL' ELSE 'OPEN' END,
                    version = version + 1, updated_at = now()
                WHERE id = :id RETURNING *
                """), input, ends).param("id", activity.id()).query(Activity.class).single();
    }

    private JdbcClient.StatementSpec fields(JdbcClient.StatementSpec sql, ActivityInput input, Instant ends) {
        return sql.param("title", input.title().strip()).param("description", input.description().strip())
                .param("category", input.category().name()).param("area", input.location().approximateArea().strip())
                .param("latitude", input.location().latitude()).param("longitude", input.location().longitude())
                .param("starts", input.startsAt().atOffset(ZoneOffset.UTC))
                .param("ends", ends.atOffset(ZoneOffset.UTC)).param("maximum", input.maxParticipants());
    }

    public Activity state(UUID id, ActivityStatus status) {
        return jdbc.sql("""
                UPDATE activities SET status = ?, version = version + 1, updated_at = now()
                WHERE id = ? RETURNING *
                """).params(status.name(), id).query(Activity.class).single();
    }

    public List<UUID> participants(UUID id) {
        return jdbc.sql("SELECT user_id FROM activity_participants WHERE activity_id = ? ORDER BY joined_at, user_id")
                .param(id).query(UUID.class).list();
    }

    public Set<UUID> joinedActivities(UUID user, Collection<UUID> ids) {
        if (ids.isEmpty()) return Set.of();
        return Set.copyOf(jdbc.sql("SELECT activity_id FROM activity_participants WHERE user_id = :user AND activity_id IN (:ids)")
                .param("user", user).param("ids", ids).query(UUID.class).list());
    }

    public void addParticipant(UUID activity, UUID user) {
        jdbc.sql("INSERT INTO activity_participants (activity_id, user_id) VALUES (?, ?)").params(activity, user).update();
    }

    public boolean removeParticipant(UUID activity, UUID user) {
        return jdbc.sql("DELETE FROM activity_participants WHERE activity_id = ? AND user_id = ?")
                .params(activity, user).update() > 0;
    }

    public Activity recount(UUID id) {
        return jdbc.sql("""
                UPDATE activities SET
                    participant_count = (SELECT count(*) FROM activity_participants WHERE activity_id = :id),
                    version = version + 1, updated_at = now()
                WHERE id = :id RETURNING *
                """).param("id", id).query(Activity.class).single();
    }

    public List<Activity> mine(UUID user, Long cursor, int limit, ActivityStatus status) {
        return jdbc.sql("""
                SELECT a.* FROM activities a JOIN activity_participants p ON p.activity_id = a.id
                WHERE p.user_id = :user AND a.sequence < :cursor
                  AND (:status::text IS NULL OR
                    CASE WHEN a.status IN ('CANCELLED','FINISHED') THEN a.status
                         WHEN a.ends_at <= now() THEN 'FINISHED'
                         WHEN a.starts_at <= now() THEN 'STARTED' ELSE a.status END = :status)
                ORDER BY a.sequence DESC LIMIT :limit
                """).param("user", user).param("cursor", cursor == null ? Long.MAX_VALUE : cursor)
                .param("status", status == null ? null : status.name(), Types.VARCHAR)
                .param("limit", limit).query(Activity.class).list();
    }

    public List<Activity> nearby(double latitude, double longitude, int distanceKm, Category category,
            Instant from, Instant to, Long cursor, int limit) {
        // Search and output use the same coarse grid and 5 km buckets. No precise-radius oracle.
        return jdbc.sql("""
                SELECT * FROM activities
                WHERE status IN ('OPEN','FULL') AND starts_at > now()
                  AND starts_at >= :from AND starts_at < :to AND sequence < :cursor
                  AND (:category::text IS NULL OR category = :category)
                  AND latitude BETWEEN :latitude - (:distance + 10) / 110.0 AND :latitude + (:distance + 10) / 110.0
                  AND GREATEST(5, CEIL((6371 * ACOS(LEAST(1, GREATEST(-1,
                      SIN(RADIANS(:latitude)) * SIN(RADIANS(FLOOR(latitude * 20 + 0.5) / 20))
                      + COS(RADIANS(:latitude)) * COS(RADIANS(FLOOR(latitude * 20 + 0.5) / 20))
                      * COS(RADIANS(FLOOR(longitude * 20 + 0.5) / 20 - :longitude)))))) / 5) * 5) <= :distance
                ORDER BY sequence DESC LIMIT :limit
                """).param("latitude", latitude).param("longitude", longitude).param("distance", distanceKm)
                .param("from", from.atOffset(ZoneOffset.UTC)).param("to", to.atOffset(ZoneOffset.UTC))
                .param("category", category == null ? null : category.name(), Types.VARCHAR)
                .param("cursor", cursor == null ? Long.MAX_VALUE : cursor).param("limit", limit)
                .query(Activity.class).list();
    }

    public boolean acquireNearby(UUID user) {
        return jdbc.sql("""
                INSERT INTO nearby_limits (user_id, next_request_at) VALUES (:user, now() + interval '1 second')
                ON CONFLICT (user_id) DO UPDATE SET next_request_at = excluded.next_request_at
                WHERE nearby_limits.next_request_at <= now()
                """).param("user", user).update() > 0;
    }

    public List<Activity> due() {
        return jdbc.sql("""
                SELECT * FROM activities WHERE (status IN ('OPEN','FULL') AND starts_at <= now())
                    OR (status = 'STARTED' AND ends_at <= now())
                ORDER BY id LIMIT 100 FOR UPDATE SKIP LOCKED
                """).query(Activity.class).list();
    }

    public List<Activity> affected(UUID user, UUID other) {
        return jdbc.sql("""
                SELECT a.* FROM activities a
                WHERE a.status IN ('OPEN','FULL','STARTED')
                  AND EXISTS (SELECT 1 FROM activity_participants p WHERE p.activity_id = a.id AND p.user_id = :user)
                  AND (:other::uuid IS NULL OR EXISTS
                    (SELECT 1 FROM activity_participants p WHERE p.activity_id = a.id AND p.user_id = :other))
                ORDER BY a.id FOR UPDATE OF a
                """).param("user", user).param("other", other, Types.OTHER).query(Activity.class).list();
    }
}
