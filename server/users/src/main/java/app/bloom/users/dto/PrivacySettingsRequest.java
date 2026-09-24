package app.bloom.users.dto;

import jakarta.validation.constraints.NotNull;

public record PrivacySettingsRequest(
        @NotNull Boolean discoverable,
        @NotNull Boolean showDistance,
        @NotNull Boolean showLastSeen
) {
}
