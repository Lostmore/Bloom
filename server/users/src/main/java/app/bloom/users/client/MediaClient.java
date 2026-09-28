package app.bloom.users.client;

import app.bloom.users.dto.MediaPhotoValidationRequest;
import app.bloom.users.dto.MediaPhotoValidationResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * HTTP-клиент сервиса Media: проверка владельца и готовности фотографий профиля.
 */
@Component
public class MediaClient {
    private final RestClient http;

    public MediaClient(RestClient.Builder builder, @Value("${bloom.media.url}") String url,
            @Value("${bloom.media.token}") String token) {
        http = ServiceHttp.create(builder, url, token);
    }

    public void requireOwnedPhotos(UUID owner, List<UUID> photos) {
        if (photos.isEmpty()) {
            return;
        }
        try {
            MediaPhotoValidationResponse status = http.post().uri("/internal/media/profile-photos/validate")
                .body(new MediaPhotoValidationRequest(owner, photos)).retrieve().body(MediaPhotoValidationResponse.class);
            if (status == null || !status.valid()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photos unavailable");
            }
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Media unavailable");
        }
    }
}
