package app.bloom.users;

import static org.assertj.core.api.Assertions.assertThat;

import app.bloom.users.events.OutboxPublisher;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

@EmbeddedKafka(partitions = 1, topics = "bloom.users.v1", kraft = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KafkaDeliveryTest extends UsersIntegrationTest {
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired OutboxPublisher publisher;

    @Test
    void outboxPublishesAnActualKafkaRecordWithStableUserKey() throws Exception {
        UUID owner = user();
        Map<String, Object> properties = KafkaTestUtils.consumerProps("users-test-" + UUID.randomUUID(), "false", broker);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (var consumer = new KafkaConsumer<>(properties, new StringDeserializer(), new StringDeserializer())) {
            broker.consumeFromAnEmbeddedTopic(consumer, "bloom.users.v1");
            assertThat(publisher.publishNext()).isTrue();
            var record = KafkaTestUtils.getSingleRecord(consumer, "bloom.users.v1", Duration.ofSeconds(20));
            assertThat(record.key()).isEqualTo(owner.toString());
            var event = json.readTree(record.value());
            assertThat(event.path("type").asText()).isEqualTo("user.created");
            assertThat(event.path("eventId").asText()).isNotBlank();
            assertThat(event.path("schemaVersion").asInt()).isEqualTo(1);
            assertThat(record.value()).doesNotContain("birthDate", "latitude", "phone", "nickname");
        }
    }
}
