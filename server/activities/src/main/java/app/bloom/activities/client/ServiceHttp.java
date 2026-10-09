package app.bloom.activities.client;

import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.MDC;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Общие настройки межсервисных HTTP-вызовов: таймауты, служебный токен и идентификатор запроса.
 */
final class ServiceHttp {
    private ServiceHttp() {
    }

    static RestClient create(RestClient.Builder builder, String url, String token) {
        if (token.length() < 32) {
            throw new IllegalArgumentException("Service API tokens must contain at least 32 characters");
        }
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(3));
        return builder.baseUrl(url).requestFactory(factory)
                .defaultHeader("X-Internal-Token", token)
                .requestInterceptor((request, body, execution) -> {
                    String requestId = MDC.get("requestId");
                    if (requestId != null) {
                        request.getHeaders().set("X-Request-ID", requestId);
                    }
                    return execution.execute(request, body);
                }).build();
    }
}
