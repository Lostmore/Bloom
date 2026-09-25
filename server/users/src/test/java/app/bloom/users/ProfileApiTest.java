package app.bloom.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ProfileApiTest extends UsersIntegrationTest {
    @Test
    void profileLifecycleRejectsStaleUpdatesAndMassAssignment() throws Exception {
        UUID owner = user();
        var updated = response(as(patch("/users/me"), owner).content("""
                {"version":0,"nickname":"New name","bio":"Coffee and books"}
                """), 200);
        assertThat(updated.path("version").asInt()).isEqualTo(1);
        response(as(patch("/users/me"), owner).content("{\"version\":0,\"bio\":\"stale\"}"), 409);
        response(as(patch("/users/me"), owner).content("{\"version\":1,\"verified\":true}"), 400);
        response(as(patch("/users/me"), owner).content("{\"version\":1,\"nickname\":\"   \"}"), 400);
        assertThat(response(as(get("/users/me"), owner), 200).path("nickname").asText()).isEqualTo("New name");
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events").query(Integer.class).single()).isEqualTo(2);
    }

    @Test
    void authenticationFailsClosedIncludingRevokedAndServiceTokens() throws Exception {
        UUID user = user();
        response(get("/users/me").header("X-User-ID", user), 401);
        response(internal(get("/users/me")), 401);
        response(get("/users/me").header("Authorization", "Bearer refresh-fake"), 401);
        identityDown = true;
        response(as(get("/users/me"), user), 503);
        identityDown = false;
        ACTIVE.remove(user);
        response(as(get("/users/me"), user), 401);
    }

    @Test
    void rejectsOversizedBodiesEvenWithoutContentLength() throws Exception {
        UUID owner = user();
        var request = as(patch("/users/me"), owner)
                .content("{\"version\":0,\"bio\":\"" + "a".repeat(17000) + "\"}")
                .with(original -> {
                    var chunked = spy(original);
                    doReturn(-1L).when(chunked).getContentLengthLong();
                    return chunked;
                });
        response(request, 413);
    }

    @Test
    void publicViewHidesPrivateFieldsAndObeysVisibility() throws Exception {
        UUID viewer = user();
        UUID target = user();
        response(as(put("/users/me/location"), viewer)
                .content("{\"location\":{\"latitude\":55.75,\"longitude\":37.61}}"), 204);
        response(as(put("/users/me/location"), target)
                .content("{\"location\":{\"latitude\":55.76,\"longitude\":37.62}}"), 204);
        response(as(post("/users/me/heartbeat"), target), 204);
        var view = response(as(get("/users/" + target), viewer), 200);
        assertThat(view.has("birthDate")).isFalse();
        assertThat(view.has("location")).isFalse();
        assertThat(view.has("preferences")).isFalse();
        assertThat(view.path("distanceKm").asInt()).isEqualTo(5);
        assertThat(view.path("online").asBoolean()).isTrue();
        response(as(put("/users/me/privacy"), target)
                .content("{\"privacy\":{\"discoverable\":true,\"showDistance\":false,\"showLastSeen\":false}}"), 200);
        view = response(as(get("/users/" + target), viewer), 200);
        assertThat(view.path("distanceKm").isNull()).isTrue();
        assertThat(view.path("lastSeen").isNull()).isTrue();
        response(as(put("/users/me/privacy"), target)
                .content("{\"privacy\":{\"discoverable\":false,\"showDistance\":false,\"showLastSeen\":false}}"), 200);
        response(as(get("/users/" + target), viewer), 404);
        response(as(get("/users/me"), target), 200);
    }

    @Test
    void createRejectsMinorsAndDuplicateProfile() throws Exception {
        UUID id = user();
        String valid = "{\"nickname\":\"A\",\"birthDate\":\"2000-01-01\",\"gender\":\"MALE\",\"searchModes\":[\"FRIENDS\"]}";
        response(as(put("/users/me"), id).content(valid), 409);
        UUID minor = UUID.randomUUID();
        ACTIVE.add(minor);
        response(as(put("/users/me"), minor).content(valid.replace("2000-01-01", "2020-01-01")), 400);
        response(as(get("/users/me"), minor), 404);
    }

    @Test
    void concurrentUpdatesHaveExactlyOneWinner() throws Exception {
        UUID owner = user();
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> update(owner, start));
            var second = pool.submit(() -> update(owner, start));
            start.countDown();
            assertThat(java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
    }

    @Test
    void deletionErasesDataAndPreventsResurrection() throws Exception {
        UUID owner = user();
        UUID other = user();
        response(as(delete("/users/me"), owner), 202);
        response(as(delete("/users/me"), owner), 202);
        response(as(get("/users/me"), owner), 404);
        response(as(get("/users/" + owner), other), 404);
        assertThat(jdbc.sql("SELECT nickname FROM profiles WHERE id = ?").param(owner).query(String.class).single()).isEmpty();
        assertThat(jdbc.sql("SELECT count(*) FROM account_deletions").query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE event_type = 'user.deleted'")
                .query(Integer.class).single()).isEqualTo(1);
    }

    private int update(UUID owner, CountDownLatch start) throws Exception {
        start.await();
        return http.perform(as(patch("/users/me"), owner).content("{\"version\":0,\"bio\":\"updated\"}"))
                .andReturn().getResponse().getStatus();
    }
}
