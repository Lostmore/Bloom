package app.bloom.interactions.model;

import java.time.Instant;
import java.util.UUID;

public record Match(
        UUID id,
        UUID userA,
        UUID userB,
        MatchStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean includes(UUID user) {
        return userA.equals(user) || userB.equals(user);
    }

    public UUID partner(UUID user) {
        return userA.equals(user) ? userB : userA;
    }
}
