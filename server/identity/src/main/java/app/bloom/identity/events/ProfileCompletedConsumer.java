package app.bloom.identity.events;

import app.bloom.identity.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProfileCompletedConsumer {
    private final AccountRepository accounts;
    private final ObjectMapper json;

    public ProfileCompletedConsumer(AccountRepository accounts, ObjectMapper json) {
        this.accounts = accounts;
        this.json = json;
    }

    @KafkaListener(topics = "${bloom.events.users-topic:bloom.users.v1}", groupId = "identity-profile-completion-v1")
    @Transactional
    public void receive(String payload) throws Exception {
        var event = json.readTree(payload);
        if (!"user.profile.completed".equals(event.path("type").asText())) return;
        if (event.path("schemaVersion").asInt() != 1) throw new IllegalArgumentException("Unknown user event version");
        UUID id = UUID.fromString(event.path("userId").asText());
        // Lock the account just like refresh/moderation; duplicate delivery is harmless.
        accounts.lockById(id).ifPresent(account -> {
            if (!account.isProfileCompleted()) account.completeProfile();
        });
    }
}
