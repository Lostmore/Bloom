package app.bloom.users.controller;

import app.bloom.users.dto.CreateProfileRequest;
import app.bloom.users.dto.PublicProfile;
import app.bloom.users.dto.UpdateProfileRequest;
import app.bloom.users.model.Profile;
import app.bloom.users.service.ProfileService;
import app.bloom.users.service.VisibilityService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class ProfileController {
    private final ProfileService profiles;
    private final VisibilityService visibility;

    public ProfileController(ProfileService profiles, VisibilityService visibility) {
        this.profiles = profiles;
        this.visibility = visibility;
    }

    @PutMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public Profile create(@AuthenticationPrincipal UUID user, @Valid @RequestBody CreateProfileRequest request) {
        return profiles.create(user, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Profile onboard(@AuthenticationPrincipal UUID user, @Valid @RequestBody CreateProfileRequest request) {
        return profiles.onboard(user, request);
    }

    @GetMapping("/me")
    public Profile me(@AuthenticationPrincipal UUID user) {
        return profiles.me(user);
    }

    @PatchMapping("/me")
    public Profile update(@AuthenticationPrincipal UUID user, @Valid @RequestBody UpdateProfileRequest request) {
        return profiles.patch(user, request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void delete(@AuthenticationPrincipal UUID user) {
        profiles.delete(user);
    }

    @GetMapping("/{id}")
    public PublicProfile profile(@AuthenticationPrincipal UUID user, @PathVariable UUID id) {
        return visibility.view(user, id);
    }

    @PostMapping("/me/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat(@AuthenticationPrincipal UUID user) {
        profiles.heartbeat(user);
    }
}
