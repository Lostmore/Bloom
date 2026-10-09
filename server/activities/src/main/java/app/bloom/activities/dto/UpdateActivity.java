package app.bloom.activities.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateActivity(@Min(1) long version, @NotNull @Valid ActivityInput activity) {}
