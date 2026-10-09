package app.bloom.activities.model;

import java.time.Instant;
import java.util.UUID;

/** Persistence model; private coordinates and idempotency data never leave the service. */
public record Activity(long sequence, UUID id, UUID creatorId, UUID requestId, String requestHash,
        String title, String description, Category category, String approximateArea,
        double latitude, double longitude, Instant startsAt, Instant endsAt,
        int maxParticipants, int participantCount, ActivityStatus status, long version,
        Instant createdAt, Instant updatedAt) {

    public ActivityStatus currentStatus(Instant now) {
        if (status == ActivityStatus.CANCELLED || status == ActivityStatus.FINISHED) return status;
        if (!endsAt.isAfter(now)) return ActivityStatus.FINISHED;
        if (!startsAt.isAfter(now)) return ActivityStatus.STARTED;
        return participantCount >= maxParticipants ? ActivityStatus.FULL : ActivityStatus.OPEN;
    }
}
