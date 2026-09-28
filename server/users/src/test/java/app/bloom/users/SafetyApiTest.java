package app.bloom.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SafetyApiTest extends UsersIntegrationTest {
    @Test
    void blockIsMutualIdempotentAndOnlyOwnerCanRemoveIt() throws Exception {
        UUID first = user();
        UUID second = user();
        response(as(post("/users/" + second + "/block"), first), 204);
        response(as(post("/users/" + second + "/block"), first), 204);
        response(as(get("/users/" + second), first), 404);
        response(as(get("/users/" + first), second), 404);
        response(as(delete("/users/" + first + "/block"), second), 204);
        response(as(get("/users/" + first), second), 404);
        assertThat(response(as(get("/users/me/blocks"), first), 200).size()).isEqualTo(1);
        response(as(delete("/users/" + second + "/block"), first), 204);
        response(as(get("/users/" + second), first), 200);
        response(as(post("/users/" + first + "/block"), first), 400);
        response(as(get("/users/me/blocks?limit=101"), first), 400);
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE event_type = 'user.blocked'")
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void goAccessContractRequiresServiceCredentialAndHonorsBlocks() throws Exception {
        UUID first = user();
        UUID second = user();
        String pair = "{\"viewerId\":\"" + first + "\",\"targetId\":\"" + second + "\"}";
        response(as(post("/internal/users/can-interact"), first).content(pair), 401);
        assertThat(response(internal(post("/internal/users/can-interact")).content(pair), 200)
                .path("allowed").asBoolean()).isTrue();
        response(as(post("/users/" + first + "/block"), second), 204);
        assertThat(response(internal(post("/internal/users/can-interact")).content(pair), 200)
                .path("allowed").asBoolean()).isFalse();
        String batch = "{\"viewerId\":\"" + first + "\",\"userIds\":[\"" + second + "\"]}";
        assertThat(response(internal(post("/internal/users/profiles")).content(batch), 200).size()).isZero();
    }

    @Test
    void inactiveAccountIsNotVisibleEvenWithExistingProfile() throws Exception {
        UUID viewer = user();
        UUID target = user();
        ACTIVE.remove(target);
        response(as(get("/users/" + target), viewer), 404);
    }

    @Test
    void photoAccessRequiresAttachmentAndVisibility() throws Exception {
        UUID viewer = user();
        UUID owner = user();
        UUID photo = UUID.randomUUID();
        MEDIA.put(photo, owner);
        response(as(put("/users/me/photos"), owner)
                .content("{\"mediaIds\":[\"" + photo + "\"]}"), 200);
        String body = "{\"viewerId\":\"" + viewer + "\",\"ownerId\":\"" + owner + "\",\"mediaId\":\"" + photo + "\"}";
        assertThat(response(internal(post("/internal/users/can-view-photo")).content(body), 200)
                .path("allowed").asBoolean()).isTrue();
        response(as(post("/users/" + viewer + "/block"), owner), 204);
        assertThat(response(internal(post("/internal/users/can-view-photo")).content(body), 200)
                .path("allowed").asBoolean()).isFalse();
    }

    @Test
    void snapshotsAndReportsNeverAcceptClientTokens() throws Exception {
        UUID owner = user();
        UUID other = user();
        response(as(get("/internal/users/" + owner + "/snapshot"), owner), 401);
        assertThat(response(internal(get("/internal/users/" + owner + "/snapshot")), 200).has("birthDate")).isTrue();
        String body = "{\"targetId\":\"" + other + "\",\"reason\":\"OTHER\",\"description\":\"evidence\"}";
        String report = response(as(post("/reports"), owner).content(body), 201).path("id").asText();
        response(as(get("/internal/reports/" + report), owner), 401);
        assertThat(response(internal(get("/internal/reports/" + report)), 200).path("description").asText())
                .isEqualTo("evidence");
    }

    @Test
    void reportsArePrivateRateLimitedAndAuditedWithoutFreeTextInEvents() throws Exception {
        UUID reporter = user();
        UUID target = user();
        String report = "{\"targetId\":\"" + target + "\",\"reason\":\"SPAM\",\"description\":\"private evidence\"}";
        for (int count = 0; count < 10; count++) {
            response(as(post("/reports"), reporter).content(report), 201);
        }
        response(as(post("/reports"), reporter).content(report), 429);
        response(as(get("/reports"), reporter), 405);
        assertThat(jdbc.sql("SELECT payload::text FROM outbox_events WHERE event_type = 'report.created'")
                .query(String.class).list()).allSatisfy(payload -> assertThat(payload).doesNotContain("private evidence"));
        assertThat(jdbc.sql("SELECT count(*) FROM user_audit WHERE action = 'REPORT_CREATED'")
                .query(Integer.class).single()).isEqualTo(10);
    }
}
