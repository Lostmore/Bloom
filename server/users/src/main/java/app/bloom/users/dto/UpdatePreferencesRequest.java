package app.bloom.users.dto;

import app.bloom.users.model.Preferences;
import jakarta.validation.constraints.NotNull;

public record UpdatePreferencesRequest(
        @NotNull Preferences preferences
) {
}
