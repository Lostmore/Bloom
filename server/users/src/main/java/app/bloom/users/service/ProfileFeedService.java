package app.bloom.users.service;

import app.bloom.users.dto.ProfileFeedPage;
import app.bloom.users.repository.ProfileRepository;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Basic public-profile feed until Discovery provides personalized recommendations. */
@Service
public class ProfileFeedService {
    private static final int PAGE_SIZE = 20;
    private final ProfileRepository profiles;
    private final VisibilityService visibility;

    public ProfileFeedService(ProfileRepository profiles, VisibilityService visibility) {
        this.profiles = profiles;
        this.visibility = visibility;
    }

    public ProfileFeedPage page(UUID viewer, UUID cursor) {
        var candidates = profiles.feedCandidates(viewer, cursor, PAGE_SIZE + 1);
        boolean more = candidates.size() > PAGE_SIZE;
        var page = candidates.subList(0, Math.min(PAGE_SIZE, candidates.size()));
        // Reuse active-account, mutual-block and privacy checks; never serialize stored Profile.
        var visible = visibility.batch(viewer, new HashSet<>(page));
        return new ProfileFeedPage(visible, more ? page.getLast() : null);
    }
}
