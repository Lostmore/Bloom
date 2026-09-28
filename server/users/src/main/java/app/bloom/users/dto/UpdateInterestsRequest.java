package app.bloom.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateInterestsRequest(
        @NotNull @Size(max = 15) List<@NotBlank String> interests
) {
}
