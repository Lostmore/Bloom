package app.bloom.identity.security;

import app.bloom.identity.model.AccessStatus;
import java.util.UUID;

public record AccessIdentity(
        UUID accountId,
        UUID familyId,
        AccessStatus status
) {
}
