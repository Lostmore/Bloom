package app.bloom.users.dto;

import app.bloom.users.model.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateReportRequest(
        @NotNull UUID targetId,
        @NotNull ReportReason reason,
        @NotNull @Size(max = 2000) String description
) {
}
