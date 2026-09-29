package app.bloom.users.service;

import app.bloom.users.client.IdentityClient;
import app.bloom.users.dto.PublicProfile;
import app.bloom.users.model.Location;
import app.bloom.users.model.Profile;
import app.bloom.users.repository.ProfileRepository;
import app.bloom.users.repository.SafetyRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VisibilityService {
    private final ProfileService profiles;
    private final ProfileRepository repository;
    private final SafetyRepository safety;
    private final IdentityClient identity;

    public VisibilityService(ProfileService profiles, ProfileRepository repository, SafetyRepository safety, IdentityClient identity) {
        this.profiles = profiles;
        this.repository = repository;
        this.safety = safety;
        this.identity = identity;
    }

    public PublicProfile view(UUID viewer, UUID target) {
        return batch(viewer, Set.of(target)).stream().findFirst().orElseThrow(ProfileService::unavailable);
    }

    public List<PublicProfile> batch(UUID viewer, Set<UUID> targets) {
        Profile own = profiles.me(viewer);
        Set<UUID> requested = new HashSet<>(targets);
        requested.add(viewer);
        Set<UUID> active = identity.active(requested);
        if (!active.contains(viewer)) {
            throw ProfileService.unavailable();
        }
        Set<UUID> blocked = safety.blockedTargets(viewer, targets);
        return repository.findAll(targets).stream()
                .filter(profile -> !profile.deleted() && active.contains(profile.id()) && !blocked.contains(profile.id()))
                .filter(profile -> profile.id().equals(viewer) || profile.privacy().discoverable())
                .map(profile -> publicView(own, profile)).toList();
    }

    public boolean canInteract(UUID viewer, UUID target) {
        if (viewer.equals(target)) {
            return false;
        }
        if (safety.blocked(viewer, target)) {
            return false;
        }
        Set<UUID> ids = Set.of(viewer, target);
        if (repository.findAll(ids).stream().filter(profile -> !profile.deleted()).count() != 2) {
            return false;
        }
        return identity.active(ids).containsAll(ids);
    }

    public Set<UUID> allowedTargets(UUID viewer, Set<UUID> targets) {
        Set<UUID> ids = new HashSet<>(targets);
        ids.add(viewer);
        Set<UUID> existing = new HashSet<>();
        repository.findAll(ids).stream().filter(profile -> !profile.deleted())
                .forEach(profile -> existing.add(profile.id()));
        if (!existing.contains(viewer)) {
            return Set.of();
        }
        existing.retainAll(identity.active(ids));
        if (!existing.contains(viewer)) {
            return Set.of();
        }
        existing.remove(viewer);
        existing.removeAll(safety.blockedTargets(viewer, targets));
        return Set.copyOf(existing);
    }

    public boolean canViewPhoto(UUID viewer, UUID owner, UUID mediaId) {
        try {
            return view(viewer, owner).photos().contains(mediaId);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == 404) {
                return false;
            }
            throw exception;
        }
    }

    public Profile snapshot(UUID id) {
        Profile profile = profiles.me(id);
        if (!identity.active(Set.of(id)).contains(id)) {
            throw ProfileService.unavailable();
        }
        return profile;
    }

    private PublicProfile publicView(Profile viewer, Profile target) {
        Instant seen = target.privacy().showLastSeen() ? target.lastSeen() : null;
        Integer distance = target.privacy().showDistance() ? distance(viewer.location(), target.location()) : null;
        return new PublicProfile(target.id(), target.nickname(),
                Period.between(target.birthDate(), LocalDate.now(ZoneOffset.UTC)).getYears(), target.gender(),
                target.bio(), target.city(), target.relationshipGoal(), target.searchModes(), target.interests(),
                target.photos(), target.verified(), seen, seen != null && seen.isAfter(Instant.now().minusSeconds(120)),
                distance);
    }

    private Integer distance(Location from, Location to) {
        if (from == null || to == null) {
            return null;
        }
        double latitude = Math.toRadians(to.latitude() - from.latitude());
        double longitude = Math.toRadians(to.longitude() - from.longitude());
        double a = Math.pow(Math.sin(latitude / 2), 2)
                + Math.cos(Math.toRadians(from.latitude())) * Math.cos(Math.toRadians(to.latitude()))
                * Math.pow(Math.sin(longitude / 2), 2);
        double km = 12742 * Math.asin(Math.sqrt(Math.min(1, a)));
        // Only coarse 5 km buckets leave the service; never precise GPS.
        return Math.max(5, (int) Math.ceil(km / 5) * 5);
    }
}
