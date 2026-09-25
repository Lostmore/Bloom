package app.bloom.users.controller;

import app.bloom.users.dto.UpdateInterestsRequest;
import app.bloom.users.dto.UpdateLocationRequest;
import app.bloom.users.dto.UpdatePhotosRequest;
import app.bloom.users.dto.UpdatePreferencesRequest;
import app.bloom.users.dto.UpdatePrivacyRequest;
import app.bloom.users.model.Interest;
import app.bloom.users.model.Location;
import app.bloom.users.model.Preferences;
import app.bloom.users.model.Privacy;
import app.bloom.users.service.ProfileService;
import app.bloom.users.service.SettingsService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettingsController {
    private final SettingsService settings;
    private final ProfileService profiles;

    public SettingsController(SettingsService settings, ProfileService profiles) {
        this.settings = settings;
        this.profiles = profiles;
    }

    @GetMapping("/interests")
    public List<Interest> catalog() {
        return settings.catalog();
    }

    @GetMapping("/users/me/preferences")
    public Preferences preferences(@AuthenticationPrincipal UUID user) {
        return profiles.me(user).preferences();
    }

    @PutMapping("/users/me/preferences")
    public Preferences preferences(@AuthenticationPrincipal UUID user, @Valid @RequestBody UpdatePreferencesRequest request) {
        return settings.preferences(user, request.preferences());
    }

    @GetMapping("/users/me/privacy")
    public Privacy privacy(@AuthenticationPrincipal UUID user) {
        return profiles.me(user).privacy();
    }

    @PutMapping("/users/me/privacy")
    public Privacy privacy(@AuthenticationPrincipal UUID user, @Valid @RequestBody UpdatePrivacyRequest request) {
        var value = request.privacy();
        return settings.privacy(user, new Privacy(value.discoverable(), value.showDistance(), value.showLastSeen()));
    }

    @GetMapping("/users/me/interests")
    public List<String> interests(@AuthenticationPrincipal UUID user) {
        return profiles.me(user).interests();
    }

    @PutMapping("/users/me/interests")
    public List<String> interests(@AuthenticationPrincipal UUID user, @Valid @RequestBody UpdateInterestsRequest request) {
        return settings.interests(user, request.interests());
    }

    @PutMapping("/users/me/photos")
    public List<UUID> photos(@AuthenticationPrincipal UUID user, @Valid @RequestBody UpdatePhotosRequest request) {
        return settings.photos(user, request.mediaIds());
    }

    @PutMapping("/users/me/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void location(@AuthenticationPrincipal UUID user, @Valid @RequestBody UpdateLocationRequest request) {
        settings.location(user, new Location(request.location().latitude(), request.location().longitude()));
    }

    @DeleteMapping("/users/me/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void location(@AuthenticationPrincipal UUID user) {
        settings.location(user, null);
    }
}
