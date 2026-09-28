package app.bloom.users.service;

import app.bloom.users.client.MediaClient;
import app.bloom.users.model.Interest;
import app.bloom.users.model.Location;
import app.bloom.users.model.Preferences;
import app.bloom.users.model.Privacy;
import app.bloom.users.model.Profile;
import app.bloom.users.repository.ProfileRepository;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettingsService {
    private final ProfileService profiles;
    private final ProfileRepository repository;
    private final MediaClient media;

    public SettingsService(ProfileService profiles, ProfileRepository repository, MediaClient media) {
        this.profiles = profiles;
        this.repository = repository;
        this.media = media;
    }

    @Transactional
    public Preferences preferences(UUID id, Preferences value) {
        if (value.minAge() < 18 || value.maxAge() > 120 || value.minAge() > value.maxAge()
                || value.maxDistanceKm() < 1 || value.maxDistanceKm() > 500
                || value.preferredGenders() == null || value.preferredGenders().isEmpty()
                || value.preferredGenders().contains(null) || value.selectedModes() == null
                || value.selectedModes().isEmpty() || value.selectedModes().contains(null)) {
            throw invalid("Invalid search preferences");
        }
        Profile profile = profiles.lock(id);
        repository.preferences(id, value);
        profiles.changed(id, profile.version() + 1);
        return value;
    }

    @Transactional
    public Privacy privacy(UUID id, Privacy value) {
        Profile profile = profiles.lock(id);
        repository.privacy(id, value);
        profiles.changed(id, profile.version() + 1);
        return value;
    }

    @Transactional
    public void location(UUID id, Location value) {
        if (value != null && (!Double.isFinite(value.latitude()) ||
            !Double.isFinite(value.longitude()) ||
            Math.abs(value.latitude()) > 90 ||
            Math.abs(value.longitude()) > 180)) {

            throw invalid("Invalid location");
        }
        Profile profile = profiles.lock(id);
        repository.location(id, value);
        profiles.changed(id, profile.version() + 1);
    }

    @Transactional
    public List<String> interests(UUID id, List<String> values) {
        var known = catalog().stream().map(Interest::id).toList();
        if (new HashSet<>(values).size() != values.size() || !known.containsAll(values)) {
            throw invalid("Unknown or duplicate interests");
        }
        Profile profile = profiles.lock(id);
        repository.interests(id, values);
        profiles.changed(id, profile.version() + 1);
        return values;
    }

    @Transactional
    public List<UUID> photos(UUID id, List<UUID> values) {
        if (new HashSet<>(values).size() != values.size()) {
            throw invalid("Duplicate photos");
        }
        media.requireOwnedPhotos(id, values);
        Profile profile = profiles.lock(id);
        repository.photos(id, values);
        profiles.changed(id, profile.version() + 1);
        return values;
    }

    public List<Interest> catalog() {
        return repository.catalog();
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
