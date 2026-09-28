package app.bloom.gateway.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Каталог публичных HTTP-контрактов всех сервисов, независимо от языка реализации. */
@Component
@ConditionalOnProperty(name = "bloom.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiCatalog {
    private final Map<String, ObjectNode> specifications = new TreeMap<>();

    public OpenApiCatalog(ObjectMapper json) throws IOException {
        var resources = new PathMatchingResourcePatternResolver().getResources("classpath*:openapi/*-openapi.json");
        for (var resource : resources) {
            String id = resource.getFilename().replace("-openapi.json", "");
            if (!id.matches("[a-z][a-z0-9-]*")) {
                throw new IllegalStateException("Invalid OpenAPI service name: " + id);
            }
            try (var input = resource.getInputStream()) {
                ObjectNode specification = (ObjectNode) json.readTree(input);
                if (!specification.path("openapi").asText().startsWith("3.")
                        || !specification.path("paths").isObject()) {
                    throw new IllegalStateException("Expected OpenAPI 3 specification: " + id);
                }
                preparePublicApi(specification);
                if (specifications.putIfAbsent(id, specification) != null) {
                    throw new IllegalStateException("Duplicate OpenAPI service: " + id);
                }
            }
        }
    }

    public List<Map<String, String>> urls() {
        return specifications.entrySet().stream()
                .map(entry -> Map.of("name", entry.getValue().path("info").path("title").asText(entry.getKey()),
                        "url", "/docs/openapi/" + entry.getKey()))
                .toList();
    }

    public JsonNode specification(String id) {
        ObjectNode result = specifications.get(id);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown service");
        }
        return result;
    }

    private void preparePublicApi(ObjectNode specification) {
        specification.putArray("servers").addObject().put("url", "/api/v1");
        ObjectNode paths = (ObjectNode) specification.get("paths");
        var fields = paths.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            if (field.getKey().equals("/internal") || field.getKey().startsWith("/internal/")) {
                fields.remove();
                continue;
            }
            ObjectNode path = (ObjectNode) field.getValue();
            path.remove("servers");
            for (String method : List.of("get", "post", "put", "patch", "delete", "head", "options", "trace")) {
                if (path.get(method) instanceof ObjectNode operation) {
                    operation.remove("servers");
                }
            }
        }
        JsonNode schemes = specification.path("components").path("securitySchemes");
        if (schemes instanceof ObjectNode security) {
            security.remove("serviceToken");
        }
    }
}
