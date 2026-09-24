package app.bloom.users.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateLocationRequest(
        @NotNull @Valid CoordinatesRequest location
) {
}
