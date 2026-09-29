package app.bloom.interactions.events;

import app.bloom.interactions.repository.EventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "bloom.events.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxSchedule {
    private final OutboxPublisher publisher;
    private final EventRepository events;

    public OutboxSchedule(OutboxPublisher publisher, EventRepository events) {
        this.publisher = publisher;
        this.events = events;
    }

    @Scheduled(fixedDelay = 1000)
    public void publish() {
        for (int count = 0; count < 50 && !Thread.currentThread().isInterrupted(); count++) {
            if (!publisher.publishNext()) {
                break;
            }
        }
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    public void cleanup() {
        events.cleanup();
    }
}
