package app.bloom.gateway.docs;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnProperty(name = "bloom.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class ApiDocsController {
    private final OpenApiCatalog catalog;

    public ApiDocsController(OpenApiCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping({"/docs", "/swagger-ui.html", "/swagger-ui/index.html"})
    public ResponseEntity<Void> redirect() {
        return ResponseEntity.status(302).location(URI.create("/docs/index.html")).build();
    }

    @GetMapping("/docs/services")
    public List<Map<String, String>> services() {
        return catalog.urls();
    }

    @GetMapping("/docs/openapi/{service}")
    public Mono<JsonNode> specification(@PathVariable String service) {
        return catalog.specification(service);
    }
}
