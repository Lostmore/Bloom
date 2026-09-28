package app.bloom.identity.security;

import app.bloom.identity.exception.AuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AccessValidator validator;
    private final LoginProtection protection;
    private final byte[] serviceToken;

    public JwtAuthenticationFilter(AccessValidator validator, LoginProtection protection,
            @Value("${bloom.internal-token}") String token) {
        if (token.length() < 32) {
            throw new IllegalArgumentException("Set bloom.internal-token in application.yml: at least 32 characters");
        }
        this.validator = validator;
        this.protection = protection;
        this.serviceToken = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        try {
            if (request.getRequestURI().startsWith("/internal/")) {
                String supplied = request.getHeader("X-Internal-Token");
                if (supplied == null || !MessageDigest.isEqual(serviceToken, supplied.getBytes(StandardCharsets.UTF_8))) {
                    response.sendError(401);
                    return;
                }
                authenticate("service", "ROLE_SERVICE");
            } else {
                if (request.getRequestURI().startsWith("/auth/") && "POST".equals(request.getMethod())) {
                    protection.require("ip", request.getRemoteAddr(), 60, Duration.ofMinutes(1), false);
                }
                String header = request.getHeader("Authorization");
                if (header != null) {
                    if (!header.startsWith("Bearer ")) {
                        throw new AuthenticationException();
                    }
                    authenticate(validator.validate(header.substring(7)), "ROLE_USER");
                }
            }
        } catch (AuthenticationException exception) {
            response.sendError(401);
            return;
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == 429) {
                response.setHeader("Retry-After", "60");
            }
            response.sendError(exception.getStatusCode().value());
            return;
        }
        chain.doFilter(request, response);
    }

    private void authenticate(Object principal, String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority(authority))));
    }
}
