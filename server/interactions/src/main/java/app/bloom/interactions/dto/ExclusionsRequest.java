package app.bloom.interactions.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record ExclusionsRequest(
        @NotNull UUID userId,
        @NotNull @Size(min = 1, max = 100) Set<@NotNull UUID> candidateIds
) {
}
