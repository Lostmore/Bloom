package app.bloom.users.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record InteractionBatchRequest(
        @NotNull UUID viewerId,
        @NotNull @Size(min = 1, max = 100) Set<@NotNull UUID> targetIds
) {
}
