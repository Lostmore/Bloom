package app.bloom.users.dto;

import java.util.List;
import java.util.UUID;

public record ProfileFeedPage(
        List<PublicProfile> items,
        UUID nextCursor
) {
}
