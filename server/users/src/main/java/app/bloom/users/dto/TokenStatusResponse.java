package app.bloom.users.dto;

import java.util.UUID;

public record TokenStatusResponse(
        boolean active,
        UUID accountId,
        UUID familyId,
        String status
) {
}
