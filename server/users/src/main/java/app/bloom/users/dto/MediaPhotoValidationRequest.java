package app.bloom.users.dto;

import java.util.List;
import java.util.UUID;

public record MediaPhotoValidationRequest(
        UUID ownerId,
        List<UUID> mediaIds
) {
}
