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
            server.createContext("/", exchange -> {
                String credentials = exchange.getRequestHeaders().getFirst("Authorization");
                boolean safe = exchange.getRequestHeaders().getFirst("X-Internal-Token") == null
                        && exchange.getRequestHeaders().getFirst("X-User-ID") == null
                        && "Bearer client-token".equals(credentials);
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
        registry.add("USERS_URL", () -> "http://127.0.0.1:" + USERS.getAddress().getPort());
    }

    @AfterAll
    static void close() {
        USERS.stop(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users/me", "/interests", "/reports"})
    void usersRoutesStripPrefixAndUntrustedIdentityHeaders(String path) {
        http.get().uri("/api/v1" + path)
                .header("Authorization", "Bearer client-token")
                .header("X-Internal-Token", "untrusted-service-token")
                .header("X-User-ID", "spoofed-user")
                .exchange().expectStatus().isOk().expectBody(String.class).isEqualTo(path);
    }

    @Test
    void internalApiIsNotRouted() {
        http.get().uri("/api/v1/internal/users/can-interact").exchange().expectStatus().isNotFound();
    }
}
