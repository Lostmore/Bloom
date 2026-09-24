package app.bloom.users.client;

import app.bloom.users.dto.TokenStatusResponse;
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

/**
 * HTTP-клиент сервиса Identity: проверка токенов, статуса аккаунтов и удаление аккаунта.
 */
@Component
public class IdentityClient {
    private final RestClient http;

    public IdentityClient(RestClient.Builder builder,
                          @Value("${bloom.identity.url}")
                          String url, @Value("${bloom.identity.token}")
                          String token) {
        http = ServiceHttp.create(builder, url, token);
    }

    public UUID authenticate(String token) {
        try {
            TokenStatusResponse status = http.post().uri("/internal/identity/introspect")
                .body(Map.of("token", token)).retrieve().body(TokenStatusResponse.class);
            if (status == null || !status.active() || status.accountId() == null || status.familyId() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
            }
            return status.accountId();
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    public Set<UUID> active(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        try {
            Set<UUID> result = http.post().uri("/internal/identity/active-accounts")
                .body(Map.of("userIds", ids)).retrieve().body(new ParameterizedTypeReference<>() {});
            if (result == null) {
                throw unavailable();
            }
            return result;
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    public void delete(UUID id) {
        try {
            http.delete().uri("/internal/identity/accounts/{id}", id).retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity unavailable");
    }
}
