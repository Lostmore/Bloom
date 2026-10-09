package app.bloom.activities.client;

import app.bloom.activities.dto.TokenStatusResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Interservice HTTP calls to Identity; the caller never supplies a trusted user ID. */
@Component
public class IdentityClient {
    private final RestClient http;

    public IdentityClient(RestClient.Builder builder, @Value("${bloom.identity.url}") String url, @Value("${bloom.identity.token}") String token) {
        http = ServiceHttp.create(builder, url, token);
    }

    public UUID authenticate(String token) {
        try {
            TokenStatusResponse result = http.post().uri("/internal/identity/introspect")
                    .body(Map.of("token", token)).retrieve().body(TokenStatusResponse.class);
            if (result == null || !result.active() || result.accountId() == null || result.familyId() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
            }
            return result.accountId();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity unavailable");
        }
    }
}
