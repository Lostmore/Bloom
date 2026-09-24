package app.bloom.users.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record Profile(
        UUID id,
        String nickname,
        LocalDate birthDate,
        Gender gender,
        String bio,
        String city,
        Location location,
        String relationshipGoal,
        Set<SearchMode> searchModes,
        Preferences preferences,
        Privacy privacy,
        List<String> interests,
        List<UUID> photos,
        boolean verified,
        Instant lastSeen,
        Instant createdAt,
        long version,
        boolean deleted
) {
}
