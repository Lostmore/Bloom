package app.bloom.users.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bloom.events.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class DeletionSchedule {
    private final AccountDeletionWorker worker;

    public DeletionSchedule(AccountDeletionWorker worker) {
        this.worker = worker;
    }

    @Scheduled(fixedDelay = 1000)
    public void process() {
        for (int count = 0; count < 50; count++) {
            if (!worker.processNext()) {
                break;
            }
        }
    }
}
