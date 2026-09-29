package app.bloom.interactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import app.bloom.interactions.events.OutboxPublisher;
import app.bloom.interactions.model.Reaction;
import app.bloom.interactions.service.InteractionService;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EmbeddedKafka(partitions = 1, topics = {"bloom.interactions.v1", "bloom.users.v1", "bloom.identity.v1"}, kraft = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KafkaDeliveryTest extends InteractionsIntegrationTest {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private OutboxPublisher publisher;

    @Autowired
    private InteractionService interactions;

    @DynamicPropertySource
    static void enableConsumer(DynamicPropertyRegistry registry) {
        registry.add("bloom.events.consuming-enabled", () -> true);
    }

    @Test
    void publishesMatchAndConsumesAnActualBlockEvent() throws Exception {
        UUID a = user();
        UUID b = user();
        interactions.react(a, b, Reaction.LIKE, UUID.randomUUID());
        UUID match = interactions.react(b, a, Reaction.SUPER_INTEREST, UUID.randomUUID()).matchId();
        var properties = KafkaTestUtils.consumerProps("interactions-test-" + UUID.randomUUID(), "false", broker);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (var consumer = new KafkaConsumer<>(properties, new StringDeserializer(), new StringDeserializer())) {
            broker.consumeFromAnEmbeddedTopic(consumer, "bloom.interactions.v1");
            assertThat(publisher.publishNext()).isTrue();
            var created = KafkaTestUtils.getSingleRecord(consumer, "bloom.interactions.v1", Duration.ofSeconds(20));
            assertThat(created.key()).isEqualTo(match.toString());
            assertThat(json.readTree(created.value()).path("type").asText()).isEqualTo("match.created");
            String block = json.writeValueAsString(Map.of("eventId", UUID.randomUUID(), "schemaVersion", 1,
                    "type", "user.blocked", "userId", a, "data", Map.of("targetId", b)));
            kafka.send("bloom.users.v1", a.toString(), block).get();
            await().atMost(Duration.ofSeconds(25)).untilAsserted(() ->
                    assertThat(jdbc.sql("SELECT status FROM matches WHERE id = ?").param(match)
                            .query(String.class).single()).isEqualTo("BLOCKED"));
            assertThat(publisher.publishNext()).isTrue();
            var closed = KafkaTestUtils.getSingleRecord(consumer, "bloom.interactions.v1", Duration.ofSeconds(20));
            assertThat(json.readTree(closed.value()).path("version").asInt()).isEqualTo(2);
            assertThat(json.readTree(closed.value()).path("type").asText()).isEqualTo("match.closed");
        }
    }
}
