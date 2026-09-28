package app.bloom.identity.model;

import java.util.UUID;

public record IdentityEventData(
        AccountStatus status,
        long tokenVersion,
        UUID familyId,
        String reason
) {
}
