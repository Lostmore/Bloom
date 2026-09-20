package app.bloom.identity.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtProvider(@Value("${bloom.jwt.secret}") String secret,
                       @Value("${bloom.jwt.access-token-ttl}") Duration accessTtl,
                       @Value("${bloom.jwt.refresh-token-ttl}") Duration refreshTtl) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String accessToken(UUID accountId, String phoneNumber, long tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(accountId.toString())
                .claim("phone", phoneNumber)
                .claim("tv", tokenVersion)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key).compact();
    }

    public String refreshToken(UUID sessionId, UUID accountId, UUID familyId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(sessionId.toString())
                .subject(accountId.toString())
                .claim("fid", familyId.toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .signWith(key).compact();
    }

    /** Returns null if token is invalid || expired. */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public UUID accountId(Claims c) {
        return UUID.fromString(c.getSubject());
    }

    public long tokenVersion(Claims c) {
        return c.get("tv", Long.class);
    }

    public String type(Claims c) {
        return c.get("type", String.class);
    }

    public UUID sessionId(Claims c) {
        return UUID.fromString(c.getId());
    }

    public UUID familyId(Claims c) {
        return UUID.fromString(c.get("fid", String.class));
    }

    public Duration getRefreshTtl() {
        return refreshTtl;
    }

    public long getAccessTtlSeconds() {
        return accessTtl.toSeconds();
    }
}
