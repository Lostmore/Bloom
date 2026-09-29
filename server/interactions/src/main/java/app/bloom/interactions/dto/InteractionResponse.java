package app.bloom.interactions.dto;

import app.bloom.interactions.model.Reaction;
import java.util.UUID;

public record InteractionResponse(
        UUID targetId,
        Reaction reaction,
        UUID matchId
) {
}
