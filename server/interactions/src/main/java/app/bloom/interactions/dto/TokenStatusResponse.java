package app.bloom.interactions.dto;

import java.util.UUID;

public record TokenStatusResponse(
        boolean active,
        UUID accountId,
        UUID familyId
) {
}
