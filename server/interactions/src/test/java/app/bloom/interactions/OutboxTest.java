package app.bloom.interactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import app.bloom.interactions.events.OutboxPublisher;
import app.bloom.interactions.model.Reaction;
import app.bloom.interactions.service.InteractionService;
import app.bloom.interactions.service.MatchService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class OutboxTest extends InteractionsIntegrationTest {
    @MockitoBean
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private OutboxPublisher publisher;

    @Autowired
    private InteractionService interactions;

    @Autowired
    private MatchService matches;

    @Test
    void failedDeliveryRetainsEventIdAndDoesNotOvertakeCreationWithClosure() {
        UUID a = user();
        UUID b = user();
        interactions.react(a, b, Reaction.LIKE, UUID.randomUUID());
        var result = interactions.react(b, a, Reaction.LIKE, UUID.randomUUID());
        matches.unmatch(a, result.matchId());
        String original = jdbc.sql("SELECT payload::text FROM outbox_events ORDER BY sequence LIMIT 1")
                .query(String.class).single();
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));
        assertThat(publisher.publishNext()).isTrue();
        assertThat(publisher.publishNext()).isFalse();
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE published_at IS NOT NULL")
                .query(Integer.class).single()).isZero();
        jdbc.sql("UPDATE outbox_events SET next_attempt_at = now()").update();
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        assertThat(publisher.publishNext()).isTrue();
        assertThat(jdbc.sql("SELECT payload::text FROM outbox_events WHERE published_at IS NOT NULL")
                .query(String.class).single()).isEqualTo(original);
        assertThat(publisher.publishNext()).isTrue();
        assertThat(publisher.publishNext()).isFalse();
    }
}
