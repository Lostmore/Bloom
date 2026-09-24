package app.bloom.users.security;

import app.bloom.users.client.IdentityClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {
    private final IdentityClient identity;
    private final byte[] serviceToken;

    public AuthenticationFilter(IdentityClient identity, @Value("${bloom.internal-token}") String token) {
        if (token.length() < 32) {
            throw new IllegalArgumentException("USERS_INTERNAL_API_TOKEN must contain at least 32 characters");
        }
        this.identity = identity;
        serviceToken = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        if (request.getContentLengthLong() > 16384) {
            response.sendError(413);
            return;
        }
        try {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (path.startsWith("/internal/") || path.equals("/actuator/prometheus")) {
                String supplied = request.getHeader("X-Internal-Token");
                if (supplied == null || supplied.length() > 1024
                        || !MessageDigest.isEqual(serviceToken, supplied.getBytes(StandardCharsets.UTF_8))) {
                    response.sendError(401);
                    return;
                }
                authenticate("service", "ROLE_SERVICE");
            } else if (!path.startsWith("/actuator/health")) {
                String bearer = request.getHeader("Authorization");
                if (bearer == null || !bearer.startsWith("Bearer ") || bearer.length() > 4103) {
                    response.sendError(401);
                    return;
                }
                authenticate(identity.authenticate(bearer.substring(7)), "ROLE_USER");
            }
        } catch (ResponseStatusException exception) {
            response.sendError(exception.getStatusCode().value());
            return;
        }
        if (List.of("POST", "PUT", "PATCH").contains(request.getMethod())) {
            byte[] body = request.getInputStream().readNBytes(16385);
            if (body.length > 16384) {
                response.sendError(413);
                return;
            }
            chain.doFilter(new BoundedJsonRequest(request, body), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private void authenticate(Object principal, String role) {
        SecurityContextHolder.getContext().
            setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority(role))));
    }
}
