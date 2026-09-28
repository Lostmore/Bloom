package app.bloom.users.dto;

import app.bloom.users.model.Gender;
import app.bloom.users.model.SearchMode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PublicProfile(
        UUID id,
        String nickname,
        int age,
        Gender gender,
        String bio,
        String city,
        String relationshipGoal,
        Set<SearchMode> searchModes,
        List<String> interests,
        List<UUID> photos,
        boolean verified,
        Instant lastSeen,
        boolean online,
        Integer distanceKm
) {
}
