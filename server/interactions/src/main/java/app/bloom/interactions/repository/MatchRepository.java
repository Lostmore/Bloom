package app.bloom.interactions.repository;

import app.bloom.interactions.model.Match;
import app.bloom.interactions.model.MatchStatus;
import app.bloom.interactions.model.Pair;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MatchRepository {
    private final JdbcClient jdbc;

    public MatchRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Match> find(UUID id) {
        return jdbc.sql("SELECT * FROM matches WHERE id = ?").param(id).query(Match.class).optional();
    }

    public Match create(Pair pair) {
        return jdbc.sql("""
                INSERT INTO matches (id, user_a, user_b, status)
                VALUES (?, ?, ?, 'ACTIVE') RETURNING *
                """).params(UUID.randomUUID(), pair.userA(), pair.userB()).query(Match.class).single();
    }

    public Optional<Match> close(UUID id, MatchStatus status) {
        return jdbc.sql("""
                UPDATE matches SET status = ?, version = version + 1, updated_at = now()
                WHERE id = ? AND status = 'ACTIVE' RETURNING *
                """).params(status.name(), id).query(Match.class).optional();
    }

    public List<Match> page(UUID user, UUID after, int limit) {
        return jdbc.sql("""
                SELECT * FROM matches
                WHERE (user_a = :user OR user_b = :user) AND status = 'ACTIVE'
                    AND (:after::uuid IS NULL OR id > :after)
                ORDER BY id LIMIT :limit
                """).param("user", user).param("after", after, Types.OTHER).param("limit", limit)
                .query(Match.class).list();
    }

    public List<Match> activeFor(UUID user) {
        return jdbc.sql("""
                SELECT * FROM matches WHERE (user_a = ? OR user_b = ?) AND status = 'ACTIVE'
                ORDER BY user_a, user_b
                """).params(user, user).query(Match.class).list();
    }
}
