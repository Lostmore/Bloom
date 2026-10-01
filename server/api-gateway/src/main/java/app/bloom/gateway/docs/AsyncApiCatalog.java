package app.bloom.gateway.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@ConditionalOnProperty(name = "bloom.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncApiCatalog {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final ApiDocsProperties properties;
    private final ResourceLoader resources;
    private final ObjectMapper json;
    private final WebClient http;

    public AsyncApiCatalog(ApiDocsProperties properties, ResourceLoader resources,
                           ObjectMapper json, WebClient.Builder builder) {
        this.properties = properties;
        this.resources = resources;
        this.json = json;
        this.http = builder.clone().codecs(config -> config.defaultCodecs().maxInMemorySize(MAX_BYTES)).build();
        properties.asyncServices().forEach((id, service) -> {
            if (!id.matches("[a-z][a-z0-9-]*") || service.name() == null || service.name().isBlank()
                    || service.source() == null) throw new IllegalArgumentException("Invalid AsyncAPI service: " + id);
            URI uri = URI.create(service.source());
            boolean bundled = service.source().startsWith("classpath:/asyncapi-docs/")
                    && !service.source().contains("..");
            boolean remote = List.of("http", "https").contains(uri.getScheme())
                    && uri.getHost() != null && uri.getUserInfo() == null && uri.getFragment() == null;
            if (!bundled && !remote) throw new IllegalArgumentException("Invalid AsyncAPI source: " + id);
        });
    }

    public List<Map<String, String>> urls() {
        return properties.asyncServices().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> Map.of("name", entry.getValue().name(), "url", "/docs/asyncapi/specs/" + entry.getKey()))
                .toList();
    }

    public Mono<JsonNode> specification(String id) {
        var service = properties.asyncServices().get(id);
        if (service == null) return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown service"));
        Mono<JsonNode> document;
        if (service.source().startsWith("classpath:")) {
            document = Mono.fromCallable(() -> {
                try (var stream = resources.getResource(service.source()).getInputStream()) {
                    byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
                    if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Document too large");
                    return json.readTree(bytes);
                }
            }).subscribeOn(Schedulers.boundedElastic());
        } else {
            // The caller selects a configured ID, never an arbitrary URL. No credentials are forwarded.
            document = http.get().uri(service.source()).retrieve().bodyToMono(JsonNode.class);
        }
        return document.switchIfEmpty(Mono.error(new IllegalArgumentException("Empty AsyncAPI")))
                .timeout(Duration.ofSeconds(5)).map(this::validate)
                .onErrorMap(error -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "AsyncAPI unavailable for " + service.name()));
    }

    private JsonNode validate(JsonNode document) {
        if (!document.path("asyncapi").asText().startsWith("3.")
                || !document.path("info").isObject() || !document.path("channels").isObject()) {
            throw new IllegalArgumentException("Expected AsyncAPI 3 JSON");
        }
        checkReferences(document);
        return document;
    }

    private void checkReferences(JsonNode node) {
        if (node.isObject() && node.has("$ref") && !node.path("$ref").asText().startsWith("#/")) {
            throw new IllegalArgumentException("External references must be bundled into the document");
        }
        node.forEach(this::checkReferences);
    }
}
