package app.bloom.users.controller;

import app.bloom.users.dto.ProfileFeedPage;
import app.bloom.users.service.ProfileFeedService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileFeedController {
    private final ProfileFeedService feed;

    public ProfileFeedController(ProfileFeedService feed) {
        this.feed = feed;
    }

    @GetMapping("/users/feed")
    public ProfileFeedPage feed(@AuthenticationPrincipal UUID viewer,
                               @RequestParam(required = false) UUID cursor) {
        return feed.page(viewer, cursor);
    }
}
