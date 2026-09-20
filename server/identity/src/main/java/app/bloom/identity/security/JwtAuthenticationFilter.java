package app.bloom.identity.security;

import app.bloom.identity.model.Account;
import app.bloom.identity.repository.AccountRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Validates Bearer access-token, checks tokenVersion (for logout-all).
 * Sets principal = account UUID.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwt;
    private final AccountRepository accounts;

    public JwtAuthenticationFilter(JwtProvider jwt, AccountRepository accounts) {
        this.jwt = jwt;
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
      // todo: validates jwt token
    }
}
