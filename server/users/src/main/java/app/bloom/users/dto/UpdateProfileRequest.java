package app.bloom.users.dto;

import app.bloom.users.model.Gender;
import app.bloom.users.model.SearchMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Частичное обновление: null оставляет поле без изменения,
 * пустая строка очищает фулл лишнее
 */
public record UpdateProfileRequest(
        @NotNull Long version,
        @Size(min = 1, max = 40) String nickname,
        Gender gender,
        @Size(max = 1000) String bio,
        @Size(max = 100) String city,
        @Size(max = 80) String relationshipGoal,
        @Size(min = 1, max = 4) Set<@NotNull SearchMode> searchModes
) {
}
