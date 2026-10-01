package app.bloom.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfileFeedApiTest extends UsersIntegrationTest {
    @Test
    void newProfilesAppearWithoutSyncAndPrivacyIsPreserved() throws Exception {
        UUID viewer = user();
        assertThat(response(as(get("/users/feed"), viewer), 200).path("items")).isEmpty();
        UUID visible = user();
        UUID hidden = user();
        UUID blocked = user();
        UUID inactive = user();
        UUID deleted = user();
        response(as(put("/users/me/privacy"), hidden)
                .content("{\"privacy\":{\"discoverable\":false,\"showDistance\":false,\"showLastSeen\":false}}"), 200);
        response(as(post("/users/" + viewer + "/block"), blocked), 204);
        ACTIVE.remove(inactive);
        response(as(delete("/users/me"), deleted), 202);
        var page = response(as(get("/users/feed"), viewer), 200);
        assertThat(page.path("items").size()).isEqualTo(1);
        var profile = page.path("items").get(0);
        assertThat(profile.path("id").asText()).isEqualTo(visible.toString());
        assertThat(profile.has("birthDate")).isFalse();
        assertThat(profile.has("location")).isFalse();
        assertThat(profile.has("preferences")).isFalse();
        response(get("/users/feed"), 401);
        response(as(get("/users/feed").param("cursor", "invalid"), viewer), 400);
    }

    @Test
    void paginationDoesNotRepeatOrLoseProfiles() throws Exception {
        UUID viewer = user();
        var expected = new HashSet<String>();
        for (int i = 0; i < 23; i++) expected.add(user().toString());
        var first = response(as(get("/users/feed"), viewer), 200);
        assertThat(first.path("items").size()).isEqualTo(20);
        var second = response(as(get("/users/feed").param("cursor", first.path("nextCursor").asText()), viewer), 200);
        assertThat(second.path("items").size()).isEqualTo(3);
        assertThat(second.path("nextCursor").isNull()).isTrue();
        var actual = new HashSet<String>();
        first.path("items").forEach(item -> assertThat(actual.add(item.path("id").asText())).isTrue());
        second.path("items").forEach(item -> assertThat(actual.add(item.path("id").asText())).isTrue());
        assertThat(actual).isEqualTo(expected);
    }
}
