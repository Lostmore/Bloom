package app.bloom.users.service;

import app.bloom.users.dto.CreateReportRequest;
import app.bloom.users.model.UserReport;
import app.bloom.users.repository.EventRepository;
import app.bloom.users.repository.SafetyRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SafetyService {
    private final ProfileService profiles;
    private final SafetyRepository safety;
    private final EventRepository events;

    public SafetyService(ProfileService profiles, SafetyRepository safety, EventRepository events) {
        this.profiles = profiles;
        this.safety = safety;
        this.events = events;
    }

    @Transactional
    public void block(UUID owner, UUID target) {
        requireDifferent(owner, target);
        lockPair(owner, target);
        if (safety.block(owner, target)) {
            events.append(owner, profiles.me(owner).version(), "user.blocked", Map.of("targetId", target));
            safety.audit(owner, "USER_BLOCKED", target);
        }
    }

    @Transactional
    public void unblock(UUID owner, UUID target) {
        requireDifferent(owner, target);
        profiles.lock(owner);
        if (safety.unblock(owner, target)) {
            events.append(owner, profiles.me(owner).version(), "user.unblocked", Map.of("targetId", target));
            safety.audit(owner, "USER_UNBLOCKED", target);
        }
    }

    public List<UUID> blocks(UUID owner, UUID after, int limit) {
        profiles.me(owner);
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and 100");
        }
        return safety.blocks(owner, after, limit);
    }

    @Transactional
    public UUID report(UUID reporter, CreateReportRequest request) {
        if (safety.recentReports(reporter) >= 10) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "CreateReportRequest limit reached");
        }
        UUID id = UUID.randomUUID();
        safety.report(id, reporter, request);
        events.append(reporter, profiles.me(reporter).version(), "report.created", Map.of("reportId", id));
        safety.audit(reporter, "REPORT_CREATED", id);
        return id;
    }

    public UserReport report(UUID id) {
        return safety.report(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CreateReportRequest unavailable"));
    }

    private void lockPair(UUID first, UUID second) {
        // Stable ordering also serializes deletion against block/report creation without a global lock.
        if (first.compareTo(second) < 0) {
            profiles.lock(first);
            profiles.lock(second);
        } else {
            profiles.lock(second);
            profiles.lock(first);
        }
    }

    private void requireDifferent(UUID first, UUID second) {
        if (first.equals(second)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot target yourself");
        }
    }
}
