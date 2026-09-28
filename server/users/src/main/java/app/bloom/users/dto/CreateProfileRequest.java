package app.bloom.users.dto;

import app.bloom.users.model.Gender;
import app.bloom.users.model.SearchMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

public record CreateProfileRequest(
        @NotBlank @Size(max = 40) String nickname,
        @NotNull LocalDate birthDate,
        @NotNull Gender gender,
        @NotNull @Size(min = 1, max = 4) Set<@NotNull SearchMode> searchModes
) {
}
