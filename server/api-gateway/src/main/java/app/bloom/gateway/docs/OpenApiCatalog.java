package app.bloom.gateway.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Fetches public service descriptions without blocking Gateway request threads. */
@Component
@ConditionalOnProperty(name = "bloom.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiCatalog {
    private static final List<String> METHODS = List.of(
            "get", "post", "put", "patch", "delete", "head", "options", "trace");
    private final ApiDocsProperties properties;
    private final WebClient http;
    private final AntPathMatcher paths = new AntPathMatcher();

    public OpenApiCatalog(ApiDocsProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.http = builder.clone().codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)).build();
        properties.services().forEach((id, service) -> {
            if (!id.matches("[a-z][a-z0-9-]*") || service.name() == null || service.url() == null
                    || !List.of("http", "https").contains(service.url().getScheme())
                    || service.url().getHost() == null || service.paths().isEmpty()) {
                throw new IllegalArgumentException("Invalid OpenAPI service configuration: " + id);
            }
        });
    }

    public List<Map<String, String>> urls() {
        return properties.services().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> Map.of("name", entry.getValue().name(), "url", "/docs/openapi/" + entry.getKey()))
                .toList();
    }

    public Mono<JsonNode> specification(String id) {
        ApiDocService service = properties.services().get(id);
        if (service == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown service"));
        }
        // Only configured destinations are accepted; browser credentials are never forwarded.
        return http.get().uri(service.url()).retrieve().bodyToMono(JsonNode.class)
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty OpenAPI response")))
                .timeout(Duration.ofSeconds(5))
                .map(specification -> preparePublicApi(specification, service))
                .onErrorMap(exception -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "OpenAPI unavailable for " + service.name() + ". Start the service and enable its OpenAPI 3 endpoint."));
    }

    private JsonNode preparePublicApi(JsonNode document, ApiDocService service) {
        if (!(document instanceof ObjectNode specification)
                || !specification.path("openapi").asText().startsWith("3.")
                || (specification.has("paths") && !specification.path("paths").isObject())) {
            throw new IllegalArgumentException("Expected OpenAPI 3");
        }
        ObjectNode publicPaths = specification.objectNode();
        specification.path("paths").fields().forEachRemaining(field -> {
            String source = field.getKey();
            if (source.equals("/internal") || source.startsWith("/internal/")) {
                return;
            }
            String target = service.pathMappings().getOrDefault(source, source);
            if (service.paths().stream().noneMatch(pattern -> paths.match(pattern, target))) {
                return;
            }
            if (!(field.getValue() instanceof ObjectNode path)) {
                throw new IllegalArgumentException("Invalid OpenAPI path");
            }
            path.remove("servers");
            for (String method : METHODS) {
                if (path.get(method) instanceof ObjectNode operation) {
                    operation.remove("servers");
                }
            }
            if (publicPaths.has(target)) {
                throw new IllegalArgumentException("Duplicate mapped OpenAPI path");
            }
            publicPaths.set(target, path);
        });
        specification.set("paths", publicPaths);
        specification.putArray("servers").addObject().put("url", "/api/v1");
        if (specification.path("components").path("securitySchemes") instanceof ObjectNode schemes) {
            schemes.remove(List.of("serviceToken", "moderationToken"));
        }
        return specification;
    }
}
