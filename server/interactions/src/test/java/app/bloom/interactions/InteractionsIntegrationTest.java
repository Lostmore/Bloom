package app.bloom.interactions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = "bloom.events.consuming-enabled=false")
@AutoConfigureMockMvc
abstract class InteractionsIntegrationTest {
    static final String INTERNAL = "interactions-test-internal-token-32-characters-minimum";
    static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    static final Set<String> BLOCKED = ConcurrentHashMap.newKeySet();
    static volatile boolean identityDown;
    static volatile boolean usersDown;
    static final ObjectMapper WIRE = new ObjectMapper();
    static final EmbeddedPostgres POSTGRES;
    static final HttpServer REMOTE;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().setPort(0).start();
            REMOTE = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            REMOTE.createContext("/internal/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                int code = 200;
                Object result = Map.of();
                if (!INTERNAL.equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"))) {
                    code = 401;
                } else if (path.contains("/identity/") && identityDown || path.contains("/users/") && usersDown) {
                    code = 503;
                } else if (path.contains("/accounts/") && exchange.getRequestMethod().equals("DELETE")) {
                    ACTIVE.remove(UUID.fromString(path.substring(path.lastIndexOf('/') + 1)));
                } else {
                    JsonNode body = WIRE.readTree(exchange.getRequestBody());
                    if (path.endsWith("/introspect")) {
                        String token = body.path("token").asText();
                        UUID user = ACTIVE.stream().filter(id -> token.equals("access-" + id)).findFirst().orElse(null);
                        result = user == null ? Map.of("active", false)
                                : Map.of("active", true, "accountId", user, "familyId", UUID.randomUUID());
                    } else if (path.endsWith("/active-accounts")) {
                        var ids = new HashSet<UUID>();
                        body.path("userIds").forEach(value -> ids.add(UUID.fromString(value.asText())));
                        ids.retainAll(ACTIVE);
                        result = ids;
                    } else if (path.endsWith("/can-interact")) {
                        UUID viewer = UUID.fromString(body.path("viewerId").asText());
                        UUID target = UUID.fromString(body.path("targetId").asText());
                        result = Map.of("allowed", allowed(viewer, target));
                    } else if (path.endsWith("/can-interact-batch")) {
                        UUID viewer = UUID.fromString(body.path("viewerId").asText());
                        var allowed = new HashSet<UUID>();
                        for (JsonNode id : body.path("targetIds")) {
                            UUID target = UUID.fromString(id.asText());
                            if (allowed(viewer, target)) {
                                allowed.add(target);
                            }
                        }
                        result = allowed;
                    } else {
                        code = 404;
                    }
                }
                byte[] bytes = WIRE.writeValueAsBytes(result);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(code, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            REMOTE.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                REMOTE.stop(0);
                try {
                    POSTGRES.close();
                } catch (Exception ignored) {
                    // Test process is exiting.
                }
            }));
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired JdbcClient jdbc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("bloom.internal-token", () -> INTERNAL);
        registry.add("bloom.identity.token", () -> INTERNAL);
        registry.add("bloom.users.token", () -> INTERNAL);
        registry.add("bloom.identity.url", () -> "http://127.0.0.1:" + REMOTE.getAddress().getPort());
        registry.add("bloom.users.url", () -> "http://127.0.0.1:" + REMOTE.getAddress().getPort());
        registry.add("bloom.events.scheduling-enabled", () -> false);
        registry.add("bloom.matching.daily-limit", () -> 10);
        registry.add("logging.level.root", () -> "WARN");
    }

    @BeforeEach
    void reset() {
        jdbc.sql("TRUNCATE interaction_requests, interaction_pairs, matches, account_guards, outbox_events, consumed_events CASCADE").update();
        ACTIVE.clear();
        BLOCKED.clear();
        identityDown = false;
        usersDown = false;
    }

    static boolean allowed(UUID viewer, UUID target) {
        return !viewer.equals(target) && ACTIVE.contains(viewer) && ACTIVE.contains(target)
                && !BLOCKED.contains(viewer + ":" + target) && !BLOCKED.contains(target + ":" + viewer);
    }

    UUID user() {
        UUID id = UUID.randomUUID();
        ACTIVE.add(id);
        return id;
    }

    MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, UUID user) {
        return request.header("Authorization", "Bearer access-" + user).contentType(MediaType.APPLICATION_JSON);
    }

    MockHttpServletRequestBuilder internal(MockHttpServletRequestBuilder request) {
        return request.header("X-Internal-Token", INTERNAL).contentType(MediaType.APPLICATION_JSON);
    }

    JsonNode response(MockHttpServletRequestBuilder request, int expected) throws Exception {
        byte[] bytes = http.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsByteArray();
        return bytes.length == 0 ? json.nullNode() : json.readTree(new String(bytes, StandardCharsets.UTF_8));
    }
}
