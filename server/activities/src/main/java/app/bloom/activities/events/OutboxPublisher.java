package app.bloom.activities.events;

import app.bloom.activities.repository.EventRepository;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OutboxPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxPublisher.class);
    private final EventRepository events;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate transactions;
    private final String topic;

    public OutboxPublisher(EventRepository events, KafkaTemplate<String, String> kafka,
            TransactionTemplate transactions, @Value("${bloom.events.topic}") String topic) {
        this.events = events;
        this.kafka = kafka;
        this.transactions = transactions;
        this.topic = topic;
    }

    public boolean publishNext() {
        return Boolean.TRUE.equals(transactions.execute(status -> {
            var next = events.lockNext();
            if (next.isEmpty()) {
                return false;
            }
            var event = next.get();
            try {
                kafka.send(topic, event.aggregateId().toString(), event.payload()).get(12, TimeUnit.SECONDS);
                events.published(event.sequence());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                events.retry(event);
            } catch (Exception exception) {
                events.retry(event);
                LOG.warn("Activity event delivery deferred: sequence={}, attempt={}", event.sequence(), event.attempts() + 1);
            }
            return true;
        }));
    }
}
