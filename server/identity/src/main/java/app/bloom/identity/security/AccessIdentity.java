package app.bloom.identity.security;

import java.util.UUID;

public record AccessIdentity(
        UUID accountId,
        UUID familyId
) {
}
