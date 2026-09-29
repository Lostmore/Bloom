package app.bloom.interactions.model;

import java.util.UUID;

public record PendingEvent(
        long sequence,
        UUID aggregateId,
        String eventType,
        String payload,
        int attempts
) {
}
