package app.bloom.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class RequestLimitsTest {
    private final AccessValidator validator = mock(AccessValidator.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            validator, mock(LoginProtection.class), "test-service-token-at-least-32-characters");

    @Test
    void rejectsOversizedBodyEvenWithoutContentLength() throws Exception {
        var request = new MockHttpServletRequest("POST", "/auth/login") {
            @Override public long getContentLengthLong() { return -1; }
            @Override public int getContentLength() { return -1; }
        };
        request.setContent(new byte[16385]);
        var response = new MockHttpServletResponse();
        var reachedController = new AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> reachedController.set(true));
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(reachedController).isFalse();
    }

    @Test
    void preservesAcceptedJsonForTheController() throws Exception {
        byte[] body = "{\"phone\":\"+79000000000\"}".getBytes(StandardCharsets.UTF_8);
        var request = new MockHttpServletRequest("POST", "/auth/login");
        request.setContent(body);
        var reachedController = new AtomicBoolean();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertThat(((HttpServletRequest) req).getInputStream().readAllBytes()).isEqualTo(body);
            reachedController.set(true);
        });
        assertThat(reachedController).isTrue();
    }

    @Test
    void rejectsOversizedBearerBeforeCryptographicValidation() throws Exception {
        var request = new MockHttpServletRequest("GET", "/auth/me");
        request.addHeader("Authorization", "Bearer " + "a".repeat(4097));
        var response = new MockHttpServletResponse();
        try {
            filter.doFilter(request, response, (req, res) -> { throw new AssertionError("Unexpected dispatch"); });
            assertThat(response.getStatus()).isEqualTo(401);
            verifyNoInteractions(validator);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
