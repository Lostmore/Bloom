package app.bloom.gateway;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiDocsTest {
    private static final HttpServer SERVICES = start();

    @Autowired
    private WebTestClient http;

    private static HttpServer start() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                boolean safe = exchange.getRequestHeaders().getFirst("Authorization") == null
                        && exchange.getRequestHeaders().getFirst("X-Internal-Token") == null;
                String document = """
                        {
                          "openapi":"3.0.3", "info":{"title":"Test service","version":"v1"},
                          "servers":[{"url":"http://private-service:9000"}],
                          "paths":{
                            "/auth/login":{"servers":[{"url":"http://private-service"}],
                              "post":{"servers":[{"url":"http://private-service"}],"responses":{"200":{"description":"OK"}}}},
                            "/api/rooms":{"get":{"responses":{"200":{"description":"OK"}}}},
                            "/internal/secrets":{"get":{"responses":{"200":{"description":"Private"}}}},
                            "/actuator/health":{"get":{"responses":{"200":{"description":"Health"}}}}
                          },
                          "components":{"securitySchemes":{"serviceToken":{"type":"apiKey","in":"header","name":"X-Internal-Token"}}}
                        }
                        """;
                if (exchange.getRequestURI().getPath().equals("/broken")) {
                    document = "{\"swagger\":\"2.0\",\"paths\":{}}";
                } else if (exchange.getRequestURI().getPath().equals("/empty")) {
                    document = "{\"openapi\":\"3.1.0\",\"info\":{\"title\":\"Stub\",\"version\":\"v1\"}}";
                }
                byte[] body = document.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(safe ? 200 : 403, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String base = "http://127.0.0.1:" + SERVICES.getAddress().getPort();
        registry.add("bloom.api-docs.services.identity.url", () -> base + "/identity");
        registry.add("bloom.api-docs.services.chat.url", () -> base + "/chat");
        registry.add("bloom.api-docs.services.media.url", () -> base + "/broken");
        registry.add("bloom.api-docs.services.activities.url", () -> base + "/empty");
        registry.add("bloom.api-docs.services.notifications.url", () -> "http://127.0.0.1:1/unavailable");
    }

    @AfterAll
    static void close() {
        SERVICES.stop(0);
    }

    @Test
    void selectorListsAllConfiguredServicesWithoutContactingThem() {
        http.get().uri("/docs/services").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.length()").isEqualTo(8)
                .jsonPath("$[?(@.name == 'Bloom Identity')].url").isEqualTo("/docs/openapi/identity")
                .jsonPath("$[?(@.name == 'Bloom Chat')].url").isEqualTo("/docs/openapi/chat");
    }

    @Test
    void javaDescriptionUsesGatewayAndDoesNotForwardBrowserCredentials() {
        http.get().uri("/docs/openapi/identity")
                .header("Authorization", "Bearer browser-token")
                .header("X-Internal-Token", "browser-service-token")
                .exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.servers[0].url").isEqualTo("/api/v1")
                .jsonPath("$.paths['/auth/login'].post").exists()
                .jsonPath("$.paths['/auth/login'].servers").doesNotExist()
                .jsonPath("$.paths['/auth/login'].post.servers").doesNotExist()
                .jsonPath("$.paths['/internal/secrets']").doesNotExist()
                .jsonPath("$.paths['/actuator/health']").doesNotExist()
                .jsonPath("$.paths['/api/rooms']").doesNotExist()
                .jsonPath("$.components.securitySchemes.serviceToken").doesNotExist();
    }

    @Test
    void goRoomPathIsMappedToExistingGatewayRoute() {
        http.get().uri("/docs/openapi/chat").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.paths['/conversations'].get").exists()
                .jsonPath("$.paths['/api/rooms']").doesNotExist()
                .jsonPath("$.servers[0].url").isEqualTo("/api/v1");
    }

    @Test
    void unavailableOrUnsupportedServiceDoesNotBreakOtherDocumentation() {
        http.get().uri("/docs/openapi/media").exchange().expectStatus().isEqualTo(503);
        http.get().uri("/docs/openapi/notifications").exchange().expectStatus().isEqualTo(503);
        http.get().uri("/docs/openapi/unknown").exchange().expectStatus().isNotFound();
        http.get().uri("/docs/openapi/identity").exchange().expectStatus().isOk();
    }

    @Test
    void serviceWithoutOperationsStillHasAValidDescription() {
        http.get().uri("/docs/openapi/activities").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.paths.length()").isEqualTo(0)
                .jsonPath("$.info.title").isEqualTo("Stub");
    }

    @Test
    void swaggerPageAndAssetsAreServedLocally() {
        http.get().uri("/docs").exchange().expectStatus().isFound()
                .expectHeader().valueEquals("Location", "/docs/index.html");
        for (String path : new String[]{"/docs/index.html", "/docs/initializer.js", "/docs/assets/swagger-ui.css",
                "/docs/assets/swagger-ui-bundle.js", "/docs/assets/swagger-ui-standalone-preset.js"}) {
            http.get().uri(path).exchange().expectStatus().isOk();
        }
    }
}
