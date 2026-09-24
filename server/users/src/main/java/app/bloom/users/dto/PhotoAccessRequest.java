package app.bloom.users.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PhotoAccessRequest(
        @NotNull UUID viewerId,
        @NotNull UUID ownerId,
        @NotNull UUID mediaId
) {
}
