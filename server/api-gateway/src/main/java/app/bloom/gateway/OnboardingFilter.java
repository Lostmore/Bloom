package app.bloom.gateway;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class OnboardingFilter implements GlobalFilter, Ordered {
    private static final Set<String> PUBLIC_AUTH = Set.of(
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout"
    );
    private final WebClient identity;
    private final String serviceToken;

    public OnboardingFilter(WebClient.Builder builder, @Value("${IDENTITY_URL:http://localhost:8081}") String url, @Value("${bloom.identity.token:}") String token) {
        serviceToken = token;
        identity = builder.baseUrl(url).defaultHeader("X-Internal-Token", token).build();
    }

    record TokenStatus(boolean active, String accountId, String familyId, String status) {}

    @Override public int getOrder() { return -100; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        if (!path.startsWith("/api/v1/") || (method.equals("POST") && PUBLIC_AUTH.contains(path))) {
            return chain.filter(exchange);
        }
        String header = exchange.getRequest().getHeaders().getFirst("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        if (token == null && path.equals("/api/v1/ws")) token = exchange.getRequest().getQueryParams().getFirst("token");
        if (token == null || token.isBlank() || token.length() > 4096) return reject(exchange, HttpStatus.UNAUTHORIZED);
        if (serviceToken.length() < 32) return reject(exchange, HttpStatus.SERVICE_UNAVAILABLE);
        boolean create = method.equals("POST") && path.equals("/api/v1/users");
        return identity.post().uri("/internal/identity/onboarding/introspect")
                .bodyValue(Map.of("token", token)).retrieve().bodyToMono(TokenStatus.class)
                .timeout(Duration.ofSeconds(3))
                .map(status -> !status.active() || status.accountId() == null || status.familyId() == null
                        ? HttpStatus.UNAUTHORIZED
                        : "ACTIVE".equals(status.status()) || (create && "ONBOARDING".equals(status.status()))
                            ? HttpStatus.OK : HttpStatus.FORBIDDEN)
                .defaultIfEmpty(HttpStatus.SERVICE_UNAVAILABLE)
                .onErrorReturn(HttpStatus.SERVICE_UNAVAILABLE)
                .flatMap(status -> status == HttpStatus.OK ? chain.filter(exchange) : reject(exchange, status));
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        return exchange.getResponse().setComplete();
    }
}
