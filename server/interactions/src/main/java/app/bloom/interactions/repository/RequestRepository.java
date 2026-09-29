package app.bloom.interactions.repository;

import app.bloom.interactions.dto.InteractionResponse;
import app.bloom.interactions.model.Reaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class RequestRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public RequestRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public Optional<InteractionResponse> previous(UUID actor, UUID key, UUID target, Reaction reaction) {
        return jdbc.sql("""
                SELECT target_id, reaction, response::text FROM interaction_requests
                WHERE actor_id = ? AND request_id = ?
                """).params(actor, key).query((row, index) -> {
                    if (!target.equals(row.getObject("target_id", UUID.class))
                            || !reaction.name().equals(row.getString("reaction"))) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used");
                    }
                    try {
                        return json.readValue(row.getString("response"), InteractionResponse.class);
                    } catch (JsonProcessingException exception) {
                        throw new IllegalStateException("Invalid stored interaction response", exception);
                    }
                }).optional();
    }

    public long dailyCount(UUID actor) {
        return jdbc.sql("""
                SELECT count(*) FROM interaction_requests
                WHERE actor_id = ? AND created_at > now() - interval '1 day'
                """).param(actor).query(Long.class).single();
    }

    public void save(UUID actor, UUID key, Reaction reaction, InteractionResponse response) {
        try {
            jdbc.sql("""
                    INSERT INTO interaction_requests (actor_id, request_id, target_id, reaction, response)
                    VALUES (?, ?, ?, ?, ?::jsonb)
                    """).params(actor, key, response.targetId(), reaction.name(), json.writeValueAsString(response))
                    .update();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize interaction response", exception);
        }
    }
}
