package app.bloom.users.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UserEvent(
        UUID eventId,
        int schemaVersion,
        String type,
        UUID userId,
        long version,
        Instant occurredAt,
        Map<String, Object> data
) {
}
