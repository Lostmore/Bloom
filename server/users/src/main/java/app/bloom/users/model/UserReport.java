package app.bloom.users.model;

import java.time.Instant;
import java.util.UUID;

public record UserReport(
        UUID id,
        UUID reporterId,
        UUID targetId,
        String reason,
        String description,
        Instant createdAt
) {
}
