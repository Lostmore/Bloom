package app.bloom.activities.service;

import app.bloom.activities.model.Activity;
import app.bloom.activities.model.ActivityStatus;
import app.bloom.activities.repository.ActivityRepository;
import app.bloom.activities.repository.EventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLifecycle {
    private final ActivityRepository activities;
    private final EventRepository events;

    public ActivityLifecycle(ActivityRepository activities, EventRepository events) {
        this.activities = activities; this.events = events;
    }

    /** Caller holds the activity row lock and transaction. */
    public Activity refresh(Activity activity) {
        ActivityStatus status = activity.currentStatus(Instant.now());
        if (status == activity.status()) return activity;
        Activity updated = activities.state(activity.id(), status);
        events.append(updated, "activity.status_changed", null, "LIFECYCLE");
        return updated;
    }

    public Activity cancel(Activity activity, UUID actor, String reason) {
        if (activity.status() == ActivityStatus.CANCELLED) return activity;
        Activity updated = activities.state(activity.id(), ActivityStatus.CANCELLED);
        events.append(updated, "activity.cancelled", actor, reason);
        return updated;
    }

    public Activity remove(Activity activity, UUID user, String reason) {
        if (!activities.removeParticipant(activity.id(), user)) return activity;
        Activity updated = refresh(activities.recount(activity.id()));
        events.append(updated, "activity.participant_left", user, reason);
        return updated;
    }

    @Transactional(timeout = 15)
    public void advanceDue() {
        activities.due().forEach(this::refresh);
    }
}
