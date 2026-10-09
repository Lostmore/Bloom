package app.bloom.activities.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateActivity(@NotNull UUID requestId, @NotNull @Valid ActivityInput activity) {}
