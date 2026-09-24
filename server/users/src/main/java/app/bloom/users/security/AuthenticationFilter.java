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

        } catch (ResponseStatusException exception) {
            response.sendError(exception.getStatusCode().value());
            return;
        }
        if (List.of("POST", "PUT", "PATCH").contains(request.getMethod())) {
            byte[] body = request.getInputStream().readAllBytes();

            chain.doFilter(new BoundedJsonRequest(request, body), response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
