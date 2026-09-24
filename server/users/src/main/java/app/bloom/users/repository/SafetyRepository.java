package app.bloom.users.repository;

import app.bloom.users.dto.CreateReportRequest;
import app.bloom.users.model.UserReport;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SafetyRepository {
    private final JdbcClient jdbc;

    public SafetyRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean blocked(UUID viewer, UUID target) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM user_blocks
                    WHERE (owner_id = :viewer AND target_id = :target)
                       OR (owner_id = :target AND target_id = :viewer)
                )
                """).param("viewer", viewer).param("target", target).query(Boolean.class).single();
    }

    public Set<UUID> blockedTargets(UUID viewer, Set<UUID> targets) {
        if (targets.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.sql("""
                SELECT target_id FROM user_blocks WHERE owner_id = :viewer AND target_id IN (:targets)
                UNION
                SELECT owner_id FROM user_blocks WHERE target_id = :viewer AND owner_id IN (:targets)
                """).param("viewer", viewer).param("targets", targets).query(UUID.class).list());
    }

    public boolean block(UUID owner, UUID target) {
        return jdbc.sql("""
                INSERT INTO user_blocks (owner_id, target_id) VALUES (:owner, :target)
                ON CONFLICT DO NOTHING
                """).param("owner", owner).param("target", target).update() == 1;
    }

    public boolean unblock(UUID owner, UUID target) {
        return jdbc.sql("DELETE FROM user_blocks WHERE owner_id = :owner AND target_id = :target")
                .param("owner", owner).param("target", target).update() == 1;
    }

    public List<UUID> blocks(UUID owner, UUID after, int limit) {
        return jdbc.sql("""
                SELECT target_id FROM user_blocks
                WHERE owner_id = :owner AND target_id > :after ORDER BY target_id LIMIT :limit
                """).param("owner", owner).param("after", after).param("limit", limit).query(UUID.class).list();
    }

    public int recentReports(UUID reporter) {
        return jdbc.sql("SELECT count(*) FROM reports WHERE reporter_id = :id AND created_at > :since")
                .param("id", reporter).param("since", java.sql.Timestamp.from(Instant.now().minusSeconds(86400)))
                .query(Integer.class).single();
    }

    public void report(UUID id, UUID reporter, CreateReportRequest request) {
        jdbc.sql("""
                INSERT INTO reports (id, reporter_id, target_id, reason, description)
                VALUES (:id, :reporter, :target, :reason, :description)
                """).param("id", id).param("reporter", reporter).param("target", request.targetId())
                .param("reason", request.reason().name()).param("description", request.description()).update();
    }

    public void audit(UUID actor, String action, UUID target) {
        jdbc.sql("INSERT INTO user_audit (actor_id, action, target_id) VALUES (:actor, :action, :target)")
                .param("actor", actor).param("action", action).param("target", target).update();
    }

    public Optional<UserReport> report(UUID id) {
        return jdbc.sql("SELECT id, reporter_id, target_id, reason, description, created_at FROM reports WHERE id = :id")
                .param("id", id).query(UserReport.class).optional();
    }
}
