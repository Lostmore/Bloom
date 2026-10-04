package app.bloom.gateway;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsersRoutingTest {
    private static final HttpServer USERS = start();

    @Autowired
    private WebTestClient http;

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/internal/identity/onboarding/introspect", exchange -> {
                String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(exchange.getRequestBody()).path("token").asText();
                String body = token.equals("bad") ? "{\"active\":false}"
                    : "{\"active\":true,\"accountId\":\"user\",\"familyId\":\"family\",\"status\":\""
                        + (token.equals("onboarding") ? "ONBOARDING" : "ACTIVE") + "\"}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.createContext("/", exchange -> {
                String credentials = exchange.getRequestHeaders().getFirst("Authorization");
                boolean safe = exchange.getRequestHeaders().getFirst("X-Internal-Token") == null
                        && exchange.getRequestHeaders().getFirst("X-User-ID") == null
                        && ("Bearer client-token".equals(credentials) || "Bearer onboarding".equals(credentials));
                byte[] body = exchange.getRequestURI().getPath().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(safe ? 200 : 400, body.length);
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
        registry.add("bloom.identity.token", () -> "gateway-test-token-at-least-32-characters");
        registry.add("IDENTITY_URL", () -> "http://127.0.0.1:" + USERS.getAddress().getPort());
        registry.add("USERS_URL", () -> "http://127.0.0.1:" + USERS.getAddress().getPort());
        registry.add("INTERACTIONS_URL", () -> "http://127.0.0.1:" + USERS.getAddress().getPort());
    }

    @AfterAll
    static void close() {
        USERS.stop(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users/me", "/interests", "/reports", "/matches", "/matches/123"})
    void usersRoutesStripPrefixAndUntrustedIdentityHeaders(String path) {
        http.get().uri("/api/v1" + path)
                .header("Authorization", "Bearer client-token")
                .header("X-Internal-Token", "untrusted-service-token")
                .header("X-User-ID", "spoofed-user")
                .exchange().expectStatus().isOk().expectBody(String.class).isEqualTo(path);
    }

    @Test
    void onboardingAllowsOnlyProfileCreationIncludingWebSocketGate() {
        http.post().uri("/api/v1/users").header("Authorization", "Bearer onboarding")
                .exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("/users");
        for (String path : new String[]{"/users/me", "/interests", "/conversations", "/matches", "/media/upload", "/activities"}) {
            http.get().uri("/api/v1" + path).header("Authorization", "Bearer onboarding")
                    .exchange().expectStatus().isForbidden();
        }
        http.get().uri("/api/v1/ws?token=onboarding").exchange().expectStatus().isForbidden();
        http.put().uri("/api/v1/users/me").header("Authorization", "Bearer onboarding")
                .exchange().expectStatus().isForbidden();
        http.get().uri("/api/v1/users/me").header("Authorization", "Bearer bad")
                .exchange().expectStatus().isUnauthorized();
        http.post().uri("/api/v1/users").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void internalApiIsNotRouted() {
        http.get().uri("/api/v1/internal/users/can-interact").exchange().expectStatus().isNotFound();
        http.post().uri("/api/v1/internal/interactions/can-access-match").exchange().expectStatus().isNotFound();
    }

    @Test
    void reactionsReachTheInteractionsService() {
        http.post().uri("/api/v1/interactions/123/like")
                .header("Authorization", "Bearer client-token")
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .header("X-Internal-Token", "untrusted-service-token")
                .exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("/interactions/123/like");
    }
}
