package app.bloom.activities.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bloom.events.consuming-enabled", havingValue = "true", matchIfMissing = true)
public class SafetyEventListener {
    private final ObjectMapper json;
    private final SafetyEventHandler handler;

    public SafetyEventListener(ObjectMapper json, SafetyEventHandler handler) {
        this.json = json;
        this.handler = handler;
    }

    @KafkaListener(topics = {"${bloom.events.users-topic}", "${bloom.events.identity-topic}"})
    public void receive(String payload) throws JsonProcessingException {
        handler.accept(json.readTree(payload));
    }
}
