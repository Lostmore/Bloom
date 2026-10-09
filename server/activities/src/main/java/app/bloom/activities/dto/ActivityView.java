package app.bloom.activities.dto;

import app.bloom.activities.model.ActivityStatus;
import app.bloom.activities.model.Category;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ActivityView(UUID id, UUID creatorId, String title, String description, Category category,
        Location location, Instant startsAt, Instant endsAt, int maxParticipants, int participantCount,
        List<UUID> participants, boolean joined, boolean canJoin, ActivityStatus status,
        long version, Instant createdAt, Instant updatedAt) {
    public record Location(String approximateArea, Integer distanceKm) {}
}
