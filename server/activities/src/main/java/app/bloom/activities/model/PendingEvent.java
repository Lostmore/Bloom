package app.bloom.activities.model;

import java.util.UUID;

public record PendingEvent(
        long sequence,
        UUID aggregateId,
        String eventType,
        String payload,
        int attempts
) {
}
