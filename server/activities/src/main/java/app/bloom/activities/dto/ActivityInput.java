package app.bloom.activities.dto;

import app.bloom.activities.model.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record ActivityInput(
        @NotBlank @Size(max = 120) String title,
        @NotNull @Size(max = 4000) String description,
        @NotNull Category category,
        @NotNull @Valid Location location,
        @NotNull Instant startsAt,
        Instant endsAt,
        @Min(2) @Max(100) int maxParticipants) {

    public record Location(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @NotBlank @Size(max = 200) String approximateArea) {}
}
