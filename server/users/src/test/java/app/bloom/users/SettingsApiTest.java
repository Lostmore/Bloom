package app.bloom.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SettingsApiTest extends UsersIntegrationTest {
    @Test
    void validatesPreferencesAndInterestCatalog() throws Exception {
        UUID owner = user();
        assertThat(response(as(get("/interests"), owner), 200).size()).isEqualTo(18);
        response(as(put("/users/me/interests"), owner).content("{\"interests\":[\"coffee\",\"books\"]}"), 200);
        response(as(put("/users/me/interests"), owner).content("{\"interests\":[\"unknown\"]}"), 400);
        response(as(put("/users/me/interests"), owner).content("{\"interests\":[\"coffee\",\"coffee\"]}"), 400);
        String preferences = """
                {"preferences":{"minAge":20,"maxAge":35,"maxDistanceKm":20,
                "preferredGenders":["FEMALE"],"selectedModes":["DATING","FRIENDS"]}}
                """;
        response(as(put("/users/me/preferences"), owner).content(preferences), 200);
        response(as(put("/users/me/preferences"), owner).content(preferences.replace("20,\"maxAge\":35", "40,\"maxAge\":35")), 400);
        response(as(put("/users/me/preferences"), owner).content(preferences.replace("\"minAge\":20", "\"minAge\":17")), 400);
        assertThat(response(as(get("/users/me/preferences"), owner), 200).path("minAge").asInt()).isEqualTo(20);
    }

    @Test
    void photosMustBeOwnedReadyMediaAndKeepOrder() throws Exception {
        UUID owner = user();
        UUID other = user();
        UUID photo = UUID.randomUUID();
        UUID foreign = UUID.randomUUID();
        MEDIA.put(photo, owner);
        MEDIA.put(foreign, other);
        response(as(put("/users/me/photos"), owner).content("{\"mediaIds\":[\"" + foreign + "\"]}"), 400);
        response(as(put("/users/me/photos"), owner).content("{\"mediaIds\":[\"" + photo + "\"]}"), 200);
        assertThat(response(as(get("/users/me"), owner), 200).path("photos").get(0).asText()).isEqualTo(photo.toString());
        mediaDown = true;
        response(as(put("/users/me/photos"), owner).content("{\"mediaIds\":[\"" + photo + "\"]}"), 503);
        response(as(put("/users/me/photos"), owner).content("{\"mediaIds\":[]}"), 200);
    }

    @Test
    void coordinatesAreValidatedAndCanBeRemoved() throws Exception {
        UUID owner = user();
        response(as(put("/users/me/location"), owner)
                .content("{\"location\":{\"latitude\":55}}"), 400);
        response(as(put("/users/me/privacy"), owner).content("{\"privacy\":{\"discoverable\":true}}"), 400);
        response(as(put("/users/me/location"), owner)
                .content("{\"location\":{\"latitude\":91,\"longitude\":0}}"), 400);
        response(as(put("/users/me/location"), owner)
                .content("{\"location\":{\"latitude\":55.7,\"longitude\":37.6}}"), 204);
        response(as(delete("/users/me/location"), owner), 204);
        assertThat(response(as(get("/users/me"), owner), 200).path("location").isNull()).isTrue();
    }
}
