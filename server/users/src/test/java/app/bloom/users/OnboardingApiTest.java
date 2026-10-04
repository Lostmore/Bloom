package app.bloom.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnboardingApiTest extends UsersIntegrationTest {
    @Test
    void migrationQueuesOnlyExistingCompleteLiveProfiles() throws Exception {
        UUID ready = user();
        UUID incomplete = user();
        UUID deleted = user();
        jdbc.sql("UPDATE profiles SET search_modes = '[]'::jsonb WHERE id = ?").param(incomplete).update();
        jdbc.sql("UPDATE profiles SET deleted = true WHERE id = ?").param(deleted).update();
        jdbc.sql("DELETE FROM outbox_events").update();
        try (var sql = getClass().getResourceAsStream("/db/migration/V3__profile_completion_backfill.sql")) {
            jdbc.sql(new String(sql.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).update();
        }
        assertThat(jdbc.sql("SELECT aggregate_id FROM outbox_events WHERE event_type = 'user.profile.completed'").query(UUID.class).list()).containsExactly(ready);
    }

    @Test
    void restrictedUserCanOnlyCreateOwnValidatedProfileAndRetryWithoutDuplicateEvent() throws Exception {
        UUID id = UUID.randomUUID();
        ACTIVE.add(id);
        ONBOARDING.add(id);
        response(as(get("/users/me"), id), 403);
        response(as(put("/users/me"), id).content("{}"), 403);
        response(as(post("/users"), id).content("{}"), 400);
        String valid = """
                {"nickname":"Anna","birthDate":"2000-01-01","gender":"FEMALE","searchModes":["DATING"]}
                """;
        response(as(post("/users"), id).content(valid.replace("2000", "2020")), 400);
        response(as(post("/users"), id).content(valid.replace("\"nickname\"", "\"user_id\":\"" + UUID.randomUUID() + "\",\"nickname\"")), 400);
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events").query(Integer.class).single()).isZero();
        assertThat(response(as(post("/users"), id).content(valid), 201).path("id").asText()).isEqualTo(id.toString());
        response(as(post("/users"), id).content(valid), 201);
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE event_type = 'user.profile.completed'")
                .query(Integer.class).single()).isEqualTo(1);
        response(as(get("/users/me"), id), 403);
        ONBOARDING.remove(id); // Refreshed ACTIVE token after Identity consumes the event.
        response(as(get("/users/me"), id), 200);
    }
}
