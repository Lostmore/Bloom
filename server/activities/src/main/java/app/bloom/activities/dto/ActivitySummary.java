package app.bloom.activities.dto;

import app.bloom.activities.model.ActivityStatus;
import app.bloom.activities.model.Category;
import java.time.Instant;
import java.util.UUID;

/** Lists do not disclose participant identities; details are checked separately. */
public record ActivitySummary(UUID id, UUID creatorId, String title, String description, Category category,
        ActivityView.Location location, Instant startsAt, Instant endsAt, int maxParticipants,
        int participantCount, boolean joined, ActivityStatus status, long version,
        Instant createdAt, Instant updatedAt) {}
