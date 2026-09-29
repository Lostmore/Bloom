package app.bloom.interactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import app.bloom.interactions.events.SafetyEventHandler;
import app.bloom.interactions.service.InteractionService;
import app.bloom.interactions.model.Reaction;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InteractionApiTest extends InteractionsIntegrationTest {
    @Autowired
    private InteractionService service;

    @Autowired
    private SafetyEventHandler safety;

    UUID match(UUID first, UUID second) throws Exception {
        react(first, second, "like", UUID.randomUUID(), 200);
        return UUID.fromString(react(second, first, "super-interest", UUID.randomUUID(), 200).path("matchId").asText());
    }

    com.fasterxml.jackson.databind.JsonNode react(UUID actor, UUID target, String action, UUID key, int code)
            throws Exception {
        return response(as(post("/interactions/" + target + "/" + action), actor)
                .header("Idempotency-Key", key), code);
    }

    @Test
    void mutualInterestCreatesOneMatchAndPrivateOutboxEvent() throws Exception {
        UUID a = user();
        UUID b = user();
        UUID key = UUID.randomUUID();
        var first = react(a, b, "like", key, 200);
        assertThat(first.path("matchId").isNull()).isTrue();
        UUID match = UUID.fromString(react(b, a, "like", UUID.randomUUID(), 200).path("matchId").asText());
        assertThat(react(a, b, "like", key, 200)).isEqualTo(first);
        react(a, b, "super-interest", UUID.randomUUID(), 200);
        assertThat(jdbc.sql("SELECT count(*) FROM matches").query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events").query(Integer.class).single()).isEqualTo(1);
        assertThat(response(as(get("/matches/" + match), a), 200).path("status").asText()).isEqualTo("ACTIVE");
        UUID outsider = user();
        response(as(get("/matches/" + match), outsider), 404);
        response(as(delete("/matches/" + match), outsider), 404);
        assertThat(response(as(get("/matches"), outsider), 200).path("items").size()).isZero();
    }

    @Test
    void skipDoesNotMatchAndCanBeReplacedByANewLike() throws Exception {
        UUID a = user();
        UUID b = user();
        react(a, b, "skip", UUID.randomUUID(), 200);
        assertThat(react(b, a, "like", UUID.randomUUID(), 200).path("matchId").isNull()).isTrue();
        assertThat(react(a, b, "like", UUID.randomUUID(), 200).path("matchId").isTextual()).isTrue();
        react(a, b, "skip", UUID.randomUUID(), 409);
    }

    @Test
    void unmatchIsIdempotentAndOldRequestsCannotReopenIt() throws Exception {
        UUID a = user();
        UUID b = user();
        UUID key = UUID.randomUUID();
        react(a, b, "like", key, 200);
        UUID id = UUID.fromString(react(b, a, "like", UUID.randomUUID(), 200).path("matchId").asText());
        response(as(delete("/matches/" + id), b), 204);
        response(as(delete("/matches/" + id), b), 204);
        response(as(get("/matches/" + id), a), 404);
        react(a, b, "like", key, 200);
        react(a, b, "like", UUID.randomUUID(), 409);
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE event_type = 'match.closed'")
                .query(Integer.class).single()).isEqualTo(1);
        jdbc.sql("UPDATE interaction_pairs SET cooldown_until = now() - interval '1 second'").update();
        assertThat(react(a, b, "like", UUID.randomUUID(), 200).path("matchId").isNull()).isTrue();
        UUID next = UUID.fromString(react(b, a, "like", UUID.randomUUID(), 200).path("matchId").asText());
        assertThat(next).isNotEqualTo(id);
        response(as(delete("/matches/" + id), a), 204);
        response(as(get("/matches/" + next), a), 200);
    }

    @Test
    void authenticationValidationAndDependencyFailureFailClosed() throws Exception {
        UUID a = user();
        UUID b = user();
        response(post("/interactions/" + b + "/like"), 401);
        response(as(post("/interactions/" + b + "/like"), a), 400);
        react(a, a, "like", UUID.randomUUID(), 400);
        UUID key = UUID.randomUUID();
        react(a, b, "like", key, 200);
        react(a, b, "skip", key, 409);
        react(a, user(), "like", key, 409);
        BLOCKED.add(b + ":" + a);
        react(a, b, "like", UUID.randomUUID(), 404);
        BLOCKED.clear();
        usersDown = true;
        react(a, b, "like", UUID.randomUUID(), 503);
        usersDown = false;
        identityDown = true;
        react(a, b, "like", UUID.randomUUID(), 503);
        identityDown = false;
        ACTIVE.remove(a);
        react(a, b, "like", UUID.randomUUID(), 401);
        response(as(get("/matches?limit=51"), b), 400);
    }

    @Test
    void concurrentOppositeLikesAndDuplicateKeysProduceOneMatch() throws Exception {
        UUID a = user();
        UUID b = user();
        UUID keyA = UUID.randomUUID();
        UUID keyB = UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = java.util.stream.IntStream.range(0, 8).mapToObj(index -> executor.submit(() -> {
                start.await();
                return index % 2 == 0 ? service.react(a, b, Reaction.LIKE, keyA)
                        : service.react(b, a, Reaction.LIKE, keyB);
            })).toList();
            start.countDown();
            for (var task : tasks) {
                task.get(30, TimeUnit.SECONDS);
            }
        }
        assertThat(jdbc.sql("SELECT count(*) FROM matches").query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM interaction_requests").query(Integer.class).single()).isEqualTo(2);
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events").query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void internalAccessAndDiscoveryExclusionsHonorLiveBlocks() throws Exception {
        UUID a = user();
        UUID b = user();
        UUID id = match(a, b);
        String body = json.writeValueAsString(Map.of("matchId", id, "userId", a));
        response(as(post("/internal/interactions/can-access-match"), a).content(body), 401);
        assertThat(response(internal(post("/internal/interactions/can-access-match")).content(body), 200)
                .path("allowed").asBoolean()).isTrue();
        BLOCKED.add(a + ":" + b);
        assertThat(response(internal(post("/internal/interactions/can-access-match")).content(body), 200)
                .path("allowed").asBoolean()).isFalse();
        assertThat(response(as(get("/matches"), a), 200).path("items").size()).isZero();
        String candidates = json.writeValueAsString(Map.of("userId", a, "candidateIds", java.util.Set.of(b)));
        assertThat(response(internal(post("/internal/interactions/exclusions")).content(candidates), 200).get(0).asText())
                .isEqualTo(b.toString());
    }

    @Test
    void safetyEventsAreDeduplicatedAndDeletionPreventsNewInteractions() throws Exception {
        UUID a = user();
        UUID b = user();
        UUID id = match(a, b);
        var event = json.valueToTree(Map.of("eventId", UUID.randomUUID(), "schemaVersion", 1,
                "type", "user.blocked", "userId", a, "data", Map.of("targetId", b)));
        safety.accept(event);
        safety.accept(event);
        response(as(get("/matches/" + id), a), 404);
        assertThat(jdbc.sql("SELECT count(*) FROM consumed_events").query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE event_type = 'match.closed'")
                .query(Integer.class).single()).isEqualTo(1);
        safety.accept(json.valueToTree(Map.of("eventId", UUID.randomUUID(), "schemaVersion", 1,
                "type", "account.deleted", "userId", a)));
        react(a, user(), "like", UUID.randomUUID(), 404);
    }

    @Test
    void rateLimitAndCursorPaginationAreBounded() throws Exception {
        UUID a = user();
        for (int i = 0; i < 10; i++) {
            match(a, user());
        }
        react(a, user(), "like", UUID.randomUUID(), 429);
        var first = response(as(get("/matches?limit=6"), a), 200);
        assertThat(first.path("items").size()).isEqualTo(6);
        var next = response(as(get("/matches?limit=6&after=" + first.path("nextCursor").asText()), a), 200);
        assertThat(next.path("items").size()).isEqualTo(4);
        assertThat(next.path("nextCursor").isNull()).isTrue();
    }

    @Test
    void openApiExposesPublicEndpointsWithoutInternalContracts() throws Exception {
        var spec = response(get("/v3/api-docs/public"), 200);
        assertThat(spec.path("paths").has("/interactions/{userId}/like")).isTrue();
        assertThat(spec.path("paths").has("/matches")).isTrue();
        assertThat(spec.path("paths").has("/internal/interactions/can-access-match")).isFalse();
    }
}
