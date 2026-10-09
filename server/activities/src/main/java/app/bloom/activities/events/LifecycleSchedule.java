package app.bloom.activities.events;

import app.bloom.activities.service.ActivityLifecycle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bloom.events.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class LifecycleSchedule {
    private final ActivityLifecycle lifecycle;
    public LifecycleSchedule(ActivityLifecycle lifecycle) { this.lifecycle = lifecycle; }

    @Scheduled(fixedDelay = 5000)
    public void advance() { lifecycle.advanceDue(); }
}
