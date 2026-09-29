package app.bloom.interactions.model;

import java.time.Instant;
import java.util.UUID;

public record Pair(
        UUID userA,
        UUID userB,
        Reaction reactionA,
        Reaction reactionB,
        UUID matchId,
        Instant cooldownUntil
) {
    public static UUID first(UUID left, UUID right) {
        return left.toString().compareTo(right.toString()) < 0 ? left : right;
    }

    public static UUID second(UUID left, UUID right) {
        return first(left, right).equals(left) ? right : left;
    }

    public Reaction oppositeReaction(UUID actor) {
        return actor.equals(userA) ? reactionB : reactionA;
    }
}
