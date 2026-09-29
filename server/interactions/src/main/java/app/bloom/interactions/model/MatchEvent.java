package app.bloom.interactions.model;

import java.time.Instant;
import java.util.UUID;

public record MatchEvent(
        UUID eventId,
        int schemaVersion,
        String type,
        UUID matchId,
        long version,
        Instant occurredAt,
        MatchEventData data
) {
}
