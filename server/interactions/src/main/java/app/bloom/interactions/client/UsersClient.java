package app.bloom.interactions.client;

import app.bloom.interactions.dto.AccessResponse;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Interservice HTTP access checks against Users, including both account statuses and blocks. */
@Component
public class UsersClient {
    private final RestClient http;

    public UsersClient(RestClient.Builder builder, @Value("${bloom.users.url}") String url,
            @Value("${bloom.users.token}") String token) {
        http = ServiceHttp.create(builder, url, token);
    }

    public boolean canInteract(UUID viewer, UUID target) {
        try {
            AccessResponse result = http.post().uri("/internal/users/can-interact")
                    .body(Map.of("viewerId", viewer, "targetId", target))
                    .retrieve().body(AccessResponse.class);
            if (result == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Users unavailable");
            }
            return result.allowed();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Users unavailable");
        }
    }

    public Set<UUID> allowedTargets(UUID viewer, Set<UUID> targets) {
        if (targets.isEmpty()) {
            return Set.of();
        }
        try {
            Set<UUID> result = http.post().uri("/internal/users/can-interact-batch")
                    .body(Map.of("viewerId", viewer, "targetIds", targets))
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            if (result == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Users unavailable");
            }
            return result;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Users unavailable");
        }
    }
}
