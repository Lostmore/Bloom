package app.bloom.identity.model;

import java.util.UUID;

public record PendingEvent(
        long sequence,
        UUID aggregateId,
        String eventType,
        String payload,
        int attempts
) {
}
