package app.bloom.users.model;

import java.util.Set;

public record Preferences(
        int minAge,
        int maxAge,
        int maxDistanceKm,
        Set<Gender> preferredGenders,
        Set<SearchMode> selectedModes
) {
}
