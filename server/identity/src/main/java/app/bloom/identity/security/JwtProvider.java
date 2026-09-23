package app.bloom.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {
    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtProvider(@Value("${bloom.jwt.secret}") String secret,
            @Value("${bloom.jwt.access-token-ttl}") Duration accessTtl,
            @Value("${bloom.jwt.refresh-token-ttl}") Duration refreshTtl) {
        if (secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        if (accessTtl.isNegative() || accessTtl.isZero() || accessTtl.compareTo(Duration.ofMinutes(15)) > 0
                || refreshTtl.isNegative() || refreshTtl.isZero()) {
            throw new IllegalArgumentException("Invalid token lifetimes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String accessToken(UUID accountId, UUID familyId, long tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("bloom-identity")
                .audience().add("bloom-api").and()
                .subject(accountId.toString())
                .claim("fid", familyId.toString())
                .claim("tv", tokenVersion)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String refreshToken(UUID sessionId, UUID accountId, UUID familyId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("bloom-identity")
                .audience().add("bloom-api").and()
                .id(sessionId.toString())
                .subject(accountId.toString())
                .claim("fid", familyId.toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        try {
            var signed = Jwts.parser().verifyWith(key)
                    .requireIssuer("bloom-identity").requireAudience("bloom-api")
                    .build().parseSignedClaims(token);
            if (!"HS256".equals(signed.getHeader().getAlgorithm())) {
                return null;
            }
            Claims claims = signed.getPayload();
            if (claims.getExpiration() == null || !claims.getExpiration().after(new Date())) {
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException exception) {
            return null;
        }
    }

    public UUID accountId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public long tokenVersion(Claims claims) {
        return claims.get("tv", Long.class);
    }

    public String type(Claims claims) {
        return claims.get("type", String.class);
    }

    public UUID sessionId(Claims claims) {
        return UUID.fromString(claims.getId());
    }

    public UUID familyId(Claims claims) {
        return UUID.fromString(claims.get("fid", String.class));
    }

    public Duration getRefreshTtl() {
        return refreshTtl;
    }

    public long getAccessTtlSeconds() {
        return accessTtl.toSeconds();
    }
}
