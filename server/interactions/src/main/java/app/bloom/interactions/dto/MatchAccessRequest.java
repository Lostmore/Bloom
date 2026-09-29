package app.bloom.interactions.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MatchAccessRequest(
        @NotNull UUID matchId,
        @NotNull UUID userId
) {
}
