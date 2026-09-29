package app.bloom.interactions.events;

import app.bloom.interactions.model.MatchStatus;
import app.bloom.interactions.repository.MatchRepository;
import app.bloom.interactions.repository.PairRepository;
import app.bloom.interactions.service.MatchLifecycle;
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
    private final PairRepository pairs;
    private final MatchRepository matches;
    private final MatchLifecycle lifecycle;

    public SafetyEventHandler(JdbcClient jdbc, PairRepository pairs, MatchRepository matches, MatchLifecycle lifecycle) {
        this.jdbc = jdbc;
        this.pairs = pairs;
        this.matches = matches;
        this.lifecycle = lifecycle;
    }

    @Transactional
    public void accept(JsonNode event) {
        String type = event.path("type").asText();
        if (!TYPES.contains(type)) {
            return;
        }
        if (event.path("schemaVersion").asInt() != 1) {
            throw new IllegalArgumentException("Unsupported safety event schema");
        }
        UUID eventId = UUID.fromString(event.path("eventId").asText());
        UUID user = UUID.fromString(event.path("userId").asText());
        int inserted = jdbc.sql("INSERT INTO consumed_events (event_id) VALUES (?) ON CONFLICT DO NOTHING")
                .param(eventId).update();
        if (inserted == 0) {
            return;
        }
        if (type.equals("user.blocked")) {
            UUID target = UUID.fromString(event.path("data").path("targetId").asText());
            if (user.equals(target)) {
                throw new IllegalArgumentException("Invalid safety event pair");
            }
            pairs.lockAccounts(user, target);
            lifecycle.close(pairs.lock(user, target), MatchStatus.BLOCKED, "USER_BLOCKED");
        } else {
            pairs.lockAccount(user);
            for (var match : matches.activeFor(user)) {
                lifecycle.close(pairs.lock(match.userA(), match.userB()), MatchStatus.BLOCKED,
                        type.endsWith("deleted") ? "ACCOUNT_DELETED" : "ACCOUNT_BLOCKED");
            }
            if (type.endsWith("deleted")) {
                pairs.markDeleted(user);
            }
        }
    }
}
