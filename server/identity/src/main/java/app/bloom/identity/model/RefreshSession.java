package app.bloom.identity.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh-token session.
 * <p>
 * Each login creates a session with a new {@code familyId}.
 * On refresh the session is revoked and a new one is created in the same family.
 * If a revoked session is used again the entire family is revoked (reuse detection).
 */
@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    protected RefreshSession() { }


    /** First session in a new family (login). */
    public static RefreshSession create(UUID accountId, String tokenHash, Instant expiresAt, String deviceInfo, String ipAddress) {
        RefreshSession s = new RefreshSession();
        s.id = UUID.randomUUID();
        s.accountId = accountId;
        s.tokenHash = tokenHash;
        s.familyId = UUID.randomUUID();
        s.deviceInfo = deviceInfo;
        s.ipAddress = ipAddress;
        s.createdAt = Instant.now();
        s.expiresAt = expiresAt;
        s.revoked = false;
        return s;
    }

    /** Next session in the same family (rotation). */
    public static RefreshSession rotate(RefreshSession previous, String newTokenHash, Instant expiresAt, String deviceInfo, String ipAddress) {
        RefreshSession s = new RefreshSession();
        s.id = UUID.randomUUID();
        s.accountId = previous.accountId;
        s.tokenHash = newTokenHash;
        s.familyId = previous.familyId; // same family
        s.deviceInfo = deviceInfo;
        s.ipAddress = ipAddress;
        s.createdAt = Instant.now();
        s.expiresAt = expiresAt;
        s.revoked = false;
        return s;
    }


    public boolean isExpired() {
        return !expiresAt.isAfter(Instant.now());
    }

    public void revoke() {
        this.revoked = true;
    }

    /** Assigns the hash after token generation. */
    public RefreshSession withTokenHash(String hash) {
        this.tokenHash = hash;
        return this;
    }


    public UUID getId() {
        return id;
    }
    public UUID getAccountId() {
        return accountId;
    }
    public String getTokenHash() {
        return tokenHash;
    }
    public UUID getFamilyId() {
        return familyId;
    }
    public String getDeviceInfo() {
        return deviceInfo;
    }
    public String getIpAddress() {
        return ipAddress;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public Instant getExpiresAt() {
        return expiresAt;
    }
    public boolean isRevoked() {
        return revoked;
    }
}
