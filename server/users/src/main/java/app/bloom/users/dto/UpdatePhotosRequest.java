package app.bloom.users.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdatePhotosRequest(
        @NotNull @Size(max = 6) List<@NotNull UUID> mediaIds
) {
}
