package app.bloom.users.events;

import app.bloom.users.client.IdentityClient;
import app.bloom.users.repository.EventRepository;
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
    private final IdentityClient identity;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate transactions;
    private final String topic;

    public OutboxPublisher(EventRepository events, IdentityClient identity, KafkaTemplate<String, String> kafka,
            TransactionTemplate transactions, @Value("${bloom.events.topic}") String topic) {
        this.events = events;
        this.identity = identity;
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
                if (event.eventType().equals("user.deleted")) {
                    identity.delete(event.aggregateId());
                }
                kafka.send(topic, event.aggregateId().toString(), event.payload()).get(12, TimeUnit.SECONDS);
                events.published(event.sequence());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                events.retry(event);
            } catch (Exception exception) {
                events.retry(event);
                LOG.warn("User event delivery deferred: sequence={}, attempt={}", event.sequence(), event.attempts() + 1);
            }
            return true;
        }));
    }
}
