package app.bloom.identity.dto;

import app.bloom.identity.model.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull AccountStatus status
) {
}
