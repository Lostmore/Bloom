package app.bloom.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import app.bloom.users.events.AccountDeletionWorker;
import app.bloom.users.events.OutboxPublisher;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class OutboxTest extends UsersIntegrationTest {
    @MockitoBean KafkaTemplate<String, String> kafka;
    @Autowired OutboxPublisher publisher;
    @Autowired AccountDeletionWorker deletion;

    @Test
    void failedDeliveryRetriesSameEventAndPreservesAggregateOrder() throws Exception {
        UUID owner = user();
        response(as(patch("/users/me"), owner).content("{\"version\":0,\"bio\":\"updated\"}"), 200);
        String payload = jdbc.sql("SELECT payload::text FROM outbox_events ORDER BY sequence LIMIT 1")
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
                .query(String.class).single()).isEqualTo(payload);
        assertThat(publisher.publishNext()).isTrue();
        assertThat(publisher.publishNext()).isFalse();
    }

    @Test
    void identityDeletionRetriesIndependentlyOfKafka() throws Exception {
        UUID owner = user();
        response(as(delete("/users/me"), owner), 202);
        identityDown = true;
        assertThat(deletion.processNext()).isTrue();
        assertThat(ACTIVE).contains(owner);
        identityDown = false;
        jdbc.sql("UPDATE account_deletions SET next_attempt_at = now()").update();
        assertThat(deletion.processNext()).isTrue();
        assertThat(ACTIVE).doesNotContain(owner);
        assertThat(deletion.processNext()).isFalse();
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE published_at IS NULL")
                .query(Integer.class).single()).isEqualTo(2);
    }
}
