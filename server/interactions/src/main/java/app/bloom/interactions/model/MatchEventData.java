package app.bloom.interactions.model;

import java.util.UUID;

public record MatchEventData(
        UUID userA,
        UUID userB,
        MatchStatus status,
        String reason
) {
}
