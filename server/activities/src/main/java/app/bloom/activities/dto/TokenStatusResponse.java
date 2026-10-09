package app.bloom.activities.dto;

import java.util.UUID;

public record TokenStatusResponse(
        boolean active,
        UUID accountId,
        UUID familyId
) {
}
