package app.bloom.identity.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class ProfileConsumerConfig {
    @Bean
    DefaultErrorHandler profileConsumerErrorHandler() {
        // Никогда не выполняем автоматическую фиксацию события
        return new DefaultErrorHandler(new FixedBackOff(5000, FixedBackOff.UNLIMITED_ATTEMPTS));
    }
}
