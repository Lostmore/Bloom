package app.bloom.users.service;

import app.bloom.users.dto.CreateProfileRequest;
import app.bloom.users.dto.UpdateProfileRequest;
import app.bloom.users.model.Profile;
import app.bloom.users.repository.DeletionRepository;
import app.bloom.users.repository.EventRepository;
import app.bloom.users.repository.ProfileRepository;
import app.bloom.users.repository.SafetyRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {
    private final ProfileRepository profiles;
    private final EventRepository events;
    private final SafetyRepository safety;
    private final DeletionRepository deletions;

    public ProfileService(ProfileRepository profiles, EventRepository events, SafetyRepository safety, DeletionRepository deletions) {
        this.profiles = profiles;
        this.events = events;
        this.safety = safety;
        this.deletions = deletions;
    }

    public Profile me(UUID id) {
        return requireActive(profiles.find(id).orElseThrow(ProfileService::unavailable));
    }

    public Profile lock(UUID id) {
        return requireActive(profiles.lock(id).orElseThrow(ProfileService::unavailable));
    }

    @Transactional
    public Profile create(UUID id, CreateProfileRequest request) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (request.birthDate().isAfter(today.minusYears(18)) || request.birthDate().isBefore(today.minusYears(120))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Age must be between 18 and 120");
        }
        boolean created = profiles.create(id, text(request.nickname()), request.birthDate(), request.gender(), request.searchModes());
        if (!created) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profile already exists");
        }
        events.append(id, 0, "user.created", Map.of());
        safety.audit(id, "PROFILE_CREATED", id);
        return me(id);
    }

    @Transactional
    public Profile patch(UUID id, UpdateProfileRequest request) {
        Profile current = lock(id);
        if (request.version() != current.version()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profile changed; reload before updating");
        }
        profiles.update(new Profile(id, request.nickname() == null ? current.nickname() : text(request.nickname()),
                current.birthDate(), request.gender() == null ? current.gender() : request.gender(),
                request.bio() == null ? current.bio() : request.bio().strip(),
                request.city() == null ? current.city() : request.city().strip(), current.location(),
                request.relationshipGoal() == null ? current.relationshipGoal() : request.relationshipGoal().strip(),
                request.searchModes() == null ? current.searchModes() : request.searchModes(),
                current.preferences(), current.privacy(), current.interests(), current.photos(), current.verified(),
                current.lastSeen(), current.createdAt(), current.version(), false));
        changed(id, current.version() + 1);
        return me(id);
    }

    @Transactional
    public void delete(UUID id) {
        Profile profile = profiles.lock(id).orElseThrow(ProfileService::unavailable);
        if (profile.deleted()) {
            return;
        }
        profiles.erase(id);
        deletions.enqueue(id);
        events.append(id, profile.version() + 1, "user.deleted", Map.of());
        safety.audit(id, "PROFILE_DELETED", id);
    }

    @Transactional
    public void heartbeat(UUID id) {
        lock(id);
        profiles.heartbeat(id);
    }

    void changed(UUID id, long version) {
        events.append(id, version, "user.updated", Map.of());
    }

    static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User unavailable");
    }

    private Profile requireActive(Profile profile) {
        if (profile.deleted()) {
            throw unavailable();
        }
        return profile;
    }

    private String text(String value) {
        if (value.isBlank() || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid nickname");
        }
        return value.strip();
    }
}
