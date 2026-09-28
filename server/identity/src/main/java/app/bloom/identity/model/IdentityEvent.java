package app.bloom.identity.model;

import java.time.Instant;
import java.util.UUID;

public record IdentityEvent(
        UUID eventId,
        int schemaVersion,
        String type,
        UUID userId,
        long version,
        Instant occurredAt,
        IdentityEventData data
) {
}
