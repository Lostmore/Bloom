package app.bloom.users.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InteractionAccessRequest(
        @NotNull UUID viewerId,
        @NotNull UUID targetId
) {
}
