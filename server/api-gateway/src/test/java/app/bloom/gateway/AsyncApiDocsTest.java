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
class AsyncApiDocsTest {
    private static final HttpServer REMOTE = start();
    @Autowired WebTestClient http;

    private static HttpServer start() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                boolean safe = exchange.getRequestHeaders().getFirst("Authorization") == null
                        && exchange.getRequestHeaders().getFirst("X-Internal-Token") == null;
                String document = switch (exchange.getRequestURI().getPath()) {
                    case "/invalid" -> "{\"openapi\":\"3.0.0\"}";
                    case "/external" -> """
                            {"asyncapi":"3.0.0","info":{"title":"Unsafe","version":"1"},"channels":{},
                             "components":{"schemas":{"secret":{"$ref":"http://private/secrets"}}}}
                            """;
                    default -> "{\"asyncapi\":\"3.0.0\",\"info\":{\"title\":\"Remote\",\"version\":\"1\"},\"channels\":{}}";
                };
                byte[] bytes = document.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(safe ? 200 : 403, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        for (String id : new String[]{"remote", "invalid", "external"}) {
            registry.add("bloom.api-docs.async-services." + id + ".name", () -> id);
            registry.add("bloom.api-docs.async-services." + id + ".source",
                    () -> "http://127.0.0.1:" + REMOTE.getAddress().getPort() + "/" + id);
        }
    }

    @AfterAll static void close() { REMOTE.stop(0); }

    @Test
    void bundledChatDocumentsActualProtocolAndPublicGatewayPath() {
        http.get().uri("/docs/asyncapi/services").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$[?(@.name == 'Bloom Chat WebSocket')].url").isEqualTo("/docs/asyncapi/specs/chat");
        http.get().uri("/docs/asyncapi/specs/chat").exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.asyncapi").isEqualTo("3.0.0")
                .jsonPath("$.channels.chat.address").isEqualTo("/api/v1/ws")
                .jsonPath("$.operations.receiveMessage.action").isEqualTo("receive")
                .jsonPath("$.operations.deliverMessage.action").isEqualTo("send")
                .jsonPath("$.components.messages.RoomChanged.examples[0].payload.id").isEqualTo(0)
                .jsonPath("$.components.securitySchemes.accessToken.in").isEqualTo("query");
    }

    @Test
    void remoteDocumentDoesNotReceiveBrowserCredentialsAndErrorsAreIsolated() {
        http.get().uri("/docs/asyncapi/specs/remote").header("Authorization", "Bearer browser-token")
                .header("X-Internal-Token", "browser-secret").exchange().expectStatus().isOk();
        http.get().uri("/docs/asyncapi/specs/invalid").exchange().expectStatus().isEqualTo(503);
        http.get().uri("/docs/asyncapi/specs/external").exchange().expectStatus().isEqualTo(503);
        http.get().uri("/docs/asyncapi/specs/unknown").exchange().expectStatus().isNotFound();
        http.get().uri("/docs/asyncapi/specs/chat").exchange().expectStatus().isOk();
    }

    @Test
    void uiAndRendererAreBundledWithGateway() {
        http.get().uri("/docs/asyncapi").exchange().expectStatus().isFound()
                .expectHeader().valueEquals("Location", "/docs/asyncapi/index.html");
        for (String path : new String[]{"/docs/asyncapi/index.html", "/docs/asyncapi/initializer.js",
                "/docs/asyncapi/vendor/asyncapi-3.2.1.css", "/docs/asyncapi/vendor/asyncapi-3.2.1.js", "/docs/navigation.css"}) {
            http.get().uri(path).exchange().expectStatus().isOk();
        }
        http.get().uri("/docs/index.html").exchange().expectStatus().isOk();
    }
}
