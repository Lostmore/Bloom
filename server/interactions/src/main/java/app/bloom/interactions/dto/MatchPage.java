package app.bloom.interactions.dto;

import app.bloom.interactions.model.Match;
import java.util.List;
import java.util.UUID;

public record MatchPage(
        List<Match> items,
        UUID nextCursor
) {
}
