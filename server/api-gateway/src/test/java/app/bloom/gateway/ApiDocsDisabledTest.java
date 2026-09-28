package app.bloom.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "bloom.api-docs.enabled=false")
class ApiDocsDisabledTest {
    @Autowired
    private WebTestClient http;

    @Test
    void disablingDocsRemovesTheUiAndCatalog() {
        for (String path : new String[]{"/docs", "/docs/services", "/docs/openapi/identity", "/docs/index.html"}) {
            http.get().uri(path).exchange().expectStatus().isNotFound();
        }
        http.get().uri("/actuator/health/readiness").exchange().expectStatus().isOk();
    }
}
