package app.bloom.interactions.repository;

import app.bloom.interactions.model.Pair;
import app.bloom.interactions.model.Reaction;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PairRepository {
    private final JdbcClient jdbc;

    public PairRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean lockAccounts(UUID left, UUID right) {
        boolean first = lockAccount(Pair.first(left, right));
        boolean second = lockAccount(Pair.second(left, right));
        return first && second;
    }

    public boolean lockAccount(UUID user) {
        jdbc.sql("INSERT INTO account_guards (user_id) VALUES (?) ON CONFLICT DO NOTHING").param(user).update();
        return !jdbc.sql("SELECT deleted FROM account_guards WHERE user_id = ? FOR UPDATE")
                .param(user).query(Boolean.class).single();
    }

    public void markDeleted(UUID user) {
        jdbc.sql("UPDATE account_guards SET deleted = TRUE WHERE user_id = ?").param(user).update();
        jdbc.sql("DELETE FROM interaction_requests WHERE actor_id = ? OR target_id = ?")
                .params(user, user).update();
        jdbc.sql("""
                UPDATE interaction_pairs SET reaction_a = NULL, reaction_b = NULL
                WHERE user_a = ? OR user_b = ?
                """).params(user, user).update();
    }

    public Pair lock(UUID left, UUID right) {
        UUID first = Pair.first(left, right);
        UUID second = Pair.second(left, right);
        jdbc.sql("""
                INSERT INTO interaction_pairs (user_a, user_b) VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """).params(first, second).update();
        return jdbc.sql("SELECT * FROM interaction_pairs WHERE user_a = ? AND user_b = ? FOR UPDATE")
                .params(first, second).query(Pair.class).single();
    }

    public void react(Pair pair, UUID actor, Reaction reaction) {
        String column = actor.equals(pair.userA()) ? "reaction_a" : "reaction_b";
        jdbc.sql("UPDATE interaction_pairs SET " + column + " = ? WHERE user_a = ? AND user_b = ?")
                .params(reaction.name(), pair.userA(), pair.userB()).update();
    }

    public void match(Pair pair, UUID matchId) {
        jdbc.sql("UPDATE interaction_pairs SET match_id = ? WHERE user_a = ? AND user_b = ?")
                .params(matchId, pair.userA(), pair.userB()).update();
    }

    public void close(Pair pair, Instant until) {
        jdbc.sql("""
                UPDATE interaction_pairs SET reaction_a = NULL, reaction_b = NULL, cooldown_until = :until
                WHERE user_a = :first AND user_b = :second
                """).param("until", until.atOffset(java.time.ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("first", pair.userA()).param("second", pair.userB()).update();
    }

    public Pair restart(Pair pair) {
        jdbc.sql("""
                UPDATE interaction_pairs SET match_id = NULL, cooldown_until = NULL,
                    reaction_a = NULL, reaction_b = NULL
                WHERE user_a = ? AND user_b = ?
                """).params(pair.userA(), pair.userB()).update();
        return lock(pair.userA(), pair.userB());
    }

    public Set<UUID> excluded(UUID user, Set<UUID> candidates) {
        List<UUID> result = jdbc.sql("""
                SELECT CASE WHEN user_a = :user THEN user_b ELSE user_a END AS target
                FROM interaction_pairs
                WHERE (user_a = :user AND user_b IN (:candidates)
                    OR user_b = :user AND user_a IN (:candidates))
                  AND (cooldown_until > now()
                    OR cooldown_until IS NULL AND (match_id IS NOT NULL
                        OR CASE WHEN user_a = :user THEN reaction_a ELSE reaction_b END IS NOT NULL))
                """).param("user", user).param("candidates", candidates).query(UUID.class).list();
        return Set.copyOf(result);
    }
}
