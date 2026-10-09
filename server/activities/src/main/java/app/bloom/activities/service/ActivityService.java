package app.bloom.activities.service;

import static app.bloom.activities.service.ActivityErrors.*;

import app.bloom.activities.client.UsersClient;
import app.bloom.activities.dto.*;
import app.bloom.activities.model.*;
import app.bloom.activities.repository.ActivityRepository;
import app.bloom.activities.repository.EventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivityService {
    private final ActivityRepository activities;
    private final EventRepository events;
    private final ActivityLifecycle lifecycle;
    private final UsersClient users;
    private final ObjectMapper json;

    public ActivityService(ActivityRepository activities, EventRepository events, ActivityLifecycle lifecycle,
            UsersClient users, ObjectMapper json) {
        this.activities = activities; this.events = events; this.lifecycle = lifecycle; this.users = users; this.json = json;
    }

    @Transactional(timeout = 15)
    public ActivityView create(UUID user, CreateActivity request) {
        if (!activities.lockAccounts(user)) throw missing();
        var origin = users.snapshot(user).location();
        String hash = hash(request.activity());
        var prior = activities.byRequest(user, request.requestId());
        if (prior.isPresent()) {
            if (!hash.equals(prior.get().requestHash())) throw conflict("REQUEST_ID_REUSED");
            return view(user, prior.get(), origin);
        }
        Instant ends = validate(request.activity());
        if (activities.creationLimit(user)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "ACTIVITY_CREATION_LIMIT");
        Activity activity = activities.create(user, request.requestId(), hash, request.activity(), ends);
        events.append(activity, "activity.created", user, null);
        return view(user, activity, origin);
    }

    @Transactional(timeout = 15)
    public ActivityView update(UUID user, UUID id, UpdateActivity request) {
        Activity activity = forMutation(user, id);
        owner(user, activity);
        if (activity.version() != request.version()) throw conflict("ACTIVITY_VERSION_CONFLICT");
        if (!activity.startsAt().isAfter(Instant.now()) || terminal(activity)) throw conflict("ACTIVITY_NOT_EDITABLE");
        Instant ends = validate(request.activity());
        if (request.activity().maxParticipants() < activity.participantCount()) throw conflict("CAPACITY_BELOW_PARTICIPANTS");
        Activity updated = activities.edit(activity, request.activity(), ends);
        events.append(updated, "activity.updated", user, null);
        return view(user, updated, users.snapshot(user).location());
    }

    @Transactional(readOnly = true, timeout = 15)
    public ActivityView get(UUID user, UUID id) {
        Activity activity = activities.find(id).orElseThrow(ActivityErrors::missing);
        visible(user, activity);
        return view(user, activity, users.snapshot(user).location());
    }

    @Transactional(timeout = 15)
    public ActivityView join(UUID user, UUID id) {
        Activity activity = forMutation(user, id);
        visible(user, activity);
        var origin = users.snapshot(user).location();
        List<UUID> members = activities.participants(id);
        // Retrying a committed join succeeds even if the activity has since started.
        if (members.contains(user)) return view(user, activity, origin);
        activity = lifecycle.refresh(activity);
        if (activity.status() != ActivityStatus.OPEN) throw conflict("ACTIVITY_NOT_OPEN");
        Set<UUID> targets = new HashSet<>(members);
        if (!users.allowedTargets(user, targets).containsAll(targets)) throw missing();
        if (!activity.startsAt().isAfter(Instant.now())) throw conflict("ACTIVITY_NOT_OPEN");
        activities.addParticipant(id, user);
        Activity updated = lifecycle.refresh(activities.recount(id));
        events.append(updated, "activity.participant_joined", user, null);
        return view(user, updated, origin);
    }

    @Transactional(timeout = 15)
    public void leave(UUID user, UUID id) {
        Activity activity = forMutation(user, id);
        // A blocked member must still be able to leave. This endpoint returns no private data.
        if (!activities.participants(id).contains(user)) return;
        if (activity.creatorId().equals(user)) throw conflict("CREATOR_MUST_CANCEL_ACTIVITY");
        activity = lifecycle.refresh(activity);
        if (terminal(activity)) throw conflict("ACTIVITY_CLOSED");
        lifecycle.remove(activity, user, "USER_LEFT");
    }

    @Transactional(timeout = 15)
    public void cancel(UUID user, UUID id) {
        Activity activity = forMutation(user, id);
        owner(user, activity);
        activity = lifecycle.refresh(activity);
        if (activity.status() == ActivityStatus.FINISHED) throw conflict("ACTIVITY_FINISHED");
        lifecycle.cancel(activity, user, "CREATOR_CANCELLED");
    }

    @Transactional(timeout = 15)
    public ActivityView finish(UUID user, UUID id) {
        Activity activity = forMutation(user, id);
        owner(user, activity);
        activity = lifecycle.refresh(activity);
        if (activity.status() != ActivityStatus.FINISHED) {
            if (activity.status() != ActivityStatus.STARTED) throw conflict("ACTIVITY_NOT_STARTED");
            activity = activities.state(id, ActivityStatus.FINISHED);
            events.append(activity, "activity.status_changed", user, "CREATOR_FINISHED");
        }
        return view(user, activity, users.snapshot(user).location());
    }

    @Transactional(readOnly = true, timeout = 15)
    public ActivityPage mine(UUID user, Long cursor, int limit, ActivityStatus status) {
        var origin = users.snapshot(user).location();
        return page(user, activities.mine(user, cursor, limit + 1, status), limit, origin);
    }

    @Transactional(timeout = 15)
    public ActivityPage nearby(UUID user, int distanceKm, Category category, Instant from, Instant to, Long cursor, int limit) {
        if (distanceKm % 5 != 0) throw invalid("DISTANCE_MUST_BE_MULTIPLE_OF_5_KM");
        Instant start = from == null ? Instant.now() : from;
        Instant end = to == null ? start.plus(Duration.ofDays(31)) : to;
        if (!end.isAfter(start) || Duration.between(start, end).compareTo(Duration.ofDays(366)) > 0) {
            throw invalid("INVALID_DATE_RANGE");
        }
        if (!activities.acquireNearby(user)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "NEARBY_RATE_LIMIT");
        var origin = users.snapshot(user).location();
        if (origin == null) throw conflict("PROFILE_LOCATION_REQUIRED");
        return page(user, activities.nearby(coarse(origin.latitude()), coarse(origin.longitude()), distanceKm,
                category, start, end, cursor, limit + 1), limit, origin);
    }

    private Activity forMutation(UUID user, UUID id) {
        Activity initial = activities.find(id).orElseThrow(ActivityErrors::missing);
        if (!activities.lockAccounts(user, initial.creatorId())) throw missing();
        return activities.lock(id).orElseThrow(ActivityErrors::missing);
    }

    private void visible(UUID user, Activity activity) {
        if (!user.equals(activity.creatorId()) && !users.canInteract(user, activity.creatorId())) throw missing();
    }

    private void owner(UUID user, Activity activity) {
        if (!user.equals(activity.creatorId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CREATOR_ONLY");
    }

    private boolean terminal(Activity activity) {
        return activity.status() == ActivityStatus.CANCELLED || activity.status() == ActivityStatus.FINISHED;
    }

    private Instant validate(ActivityInput input) {
        Instant now = Instant.now();
        Instant end = input.endsAt() == null ? input.startsAt().plus(Duration.ofHours(2)) : input.endsAt();
        if (!input.startsAt().isAfter(now) || input.startsAt().isAfter(now.plus(Duration.ofDays(366)))) {
            throw invalid("START_MUST_BE_WITHIN_NEXT_YEAR");
        }
        if (!end.isAfter(input.startsAt()) || end.isAfter(input.startsAt().plus(Duration.ofDays(7)))) {
            throw invalid("DURATION_MUST_BE_WITHIN_7_DAYS");
        }
        if (!Double.isFinite(input.location().latitude()) || !Double.isFinite(input.location().longitude())) {
            throw invalid("INVALID_LOCATION");
        }
        return end;
    }

    private ActivityView view(UUID user, Activity activity, UsersClient.Location origin) {
        List<UUID> members = activities.participants(activity.id());
        Set<UUID> targets = new HashSet<>(members);
        targets.remove(user);
        Set<UUID> allowed = users.allowedTargets(user, targets);
        List<UUID> visibleMembers = members.stream().filter(id -> id.equals(user) || allowed.contains(id)).toList();
        ActivityStatus status = activity.currentStatus(Instant.now());
        boolean joined = members.contains(user);
        return new ActivityView(activity.id(), activity.creatorId(), activity.title(), activity.description(), activity.category(),
                location(activity, origin), activity.startsAt(), activity.endsAt(), activity.maxParticipants(), activity.participantCount(),
                visibleMembers, joined, !joined && status == ActivityStatus.OPEN && allowed.containsAll(targets),
                status, activity.version(), activity.createdAt(), activity.updatedAt());
    }

    private ActivityPage page(UUID user, List<Activity> rows, int limit, UsersClient.Location origin) {
        List<Activity> scanned = rows.stream().limit(limit).toList();
        Set<UUID> creators = scanned.stream().map(Activity::creatorId).filter(id -> !id.equals(user)).collect(Collectors.toSet());
        Set<UUID> allowed = users.allowedTargets(user, creators);
        Set<UUID> joined = activities.joinedActivities(user, scanned.stream().map(Activity::id).toList());
        var items = scanned.stream().filter(a -> a.creatorId().equals(user) || allowed.contains(a.creatorId()))
                .map(a -> new ActivitySummary(a.id(), a.creatorId(), a.title(), a.description(), a.category(), location(a, origin),
                        a.startsAt(), a.endsAt(), a.maxParticipants(), a.participantCount(), joined.contains(a.id()),
                        a.currentStatus(Instant.now()), a.version(), a.createdAt(), a.updatedAt())).toList();
        Long next = rows.size() > limit ? scanned.getLast().sequence() : null;
        return new ActivityPage(items, next);
    }

    private ActivityView.Location location(Activity activity, UsersClient.Location origin) {
        if (origin == null) return new ActivityView.Location(activity.approximateArea(), null);
        double a = Math.toRadians(coarse(origin.latitude()));
        double b = Math.toRadians(coarse(activity.latitude()));
        double longitude = Math.toRadians(coarse(activity.longitude()) - coarse(origin.longitude()));
        double cosine = Math.sin(a) * Math.sin(b) + Math.cos(a) * Math.cos(b) * Math.cos(longitude);
        double distance = 6371 * Math.acos(Math.clamp(cosine, -1, 1));
        return new ActivityView.Location(activity.approximateArea(), Math.max(5, (int) Math.ceil(distance / 5) * 5));
    }

    private double coarse(double coordinate) { return Math.floor(coordinate * 20 + 0.5) / 20; }

    private String hash(ActivityInput input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(input)));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot fingerprint activity request", exception);
        }
    }
}
