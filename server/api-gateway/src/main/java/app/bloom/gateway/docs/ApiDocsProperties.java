package app.bloom.gateway.docs;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bloom.api-docs")
public record ApiDocsProperties(
        Map<String, ApiDocService> services,
        Map<String, AsyncApiDocService> asyncServices
) {
    public ApiDocsProperties {
        services = services == null ? Map.of() : Map.copyOf(services);
        asyncServices = asyncServices == null ? Map.of() : Map.copyOf(asyncServices);
    }
}
