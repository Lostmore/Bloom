package app.bloom.identity.security;

import app.bloom.identity.model.AccessStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtProvider(@Value("${bloom.jwt.private-key}") String privateKeyPem,
                       @Value("${bloom.jwt.public-key}") String publicKeyPem,
                       @Value("${bloom.jwt.access-token-ttl}") Duration accessTtl,
                       @Value("${bloom.jwt.refresh-token-ttl}") Duration refreshTtl) {
        if (accessTtl.isNegative() || accessTtl.isZero() || accessTtl.compareTo(Duration.ofMinutes(15)) > 0
                || refreshTtl.isNegative() || refreshTtl.isZero()) {
            throw new IllegalArgumentException("Invalid token lifetimes");
        }
        this.privateKey = loadPrivateKey(privateKeyPem);
        this.publicKey = loadPublicKey(publicKeyPem);
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String accessToken(UUID accountId, UUID familyId, long tokenVersion, AccessStatus status) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("bloom-identity")
                .audience().add("bloom-api").and()
                .subject(accountId.toString())
                .claim("fid", familyId.toString())
                .claim("tv", tokenVersion)
                .claim("status", status.name())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(privateKey, Jwts.SIG.RS256)
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
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims parse(String token) {
        try {
            var signed = Jwts.parser().verifyWith(publicKey)
                    .requireIssuer("bloom-identity").requireAudience("bloom-api")
                    .build().parseSignedClaims(token);
            if (!"RS256".equals(signed.getHeader().getAlgorithm())) {
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

    public AccessStatus accessStatus(Claims claims) {
        Object status = claims.get("status");
        // Tokens issued before onboarding was introduced require refresh.
        if (status == null) return AccessStatus.ONBOARDING;
        if (!(status instanceof String value)) throw new IllegalArgumentException("Invalid access status");
        return AccessStatus.valueOf(value);
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

    public String getPublicKeyPem() {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(publicKey.getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    private static PrivateKey loadPrivateKey(String pem) {
        try {
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid RSA private key", e);
        }
    }

    private static PublicKey loadPublicKey(String pem) {
        try {
            String base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid RSA public key", e);
        }
    }
}
