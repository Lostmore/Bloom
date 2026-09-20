package app.bloom.identity.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    protected Account() { }

    public static Account create(String phoneNumber, String passwordHash) {
        Account a = new Account();
        Instant now = Instant.now();
        a.id = UUID.randomUUID();
        a.phoneNumber = phoneNumber;
        a.passwordHash = passwordHash;
        a.status = AccountStatus.ACTIVE;
        a.tokenVersion = 0L;
        a.createdAt = now;
        a.updatedAt = now;
        return a;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public void incrementTokenVersion() {
        tokenVersion++;
        updatedAt = Instant.now();
    }

    public void touchLastSeen() {
        lastSeenAt = Instant.now();
    }

    public UUID getId()              { return id; }
    public String getPhoneNumber()   { return phoneNumber; }
    public String getPasswordHash()  { return passwordHash; }
    public AccountStatus getStatus() { return status; }
    public long getTokenVersion()    { return tokenVersion; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getUpdatedAt()    { return updatedAt; }
    public Instant getLastSeenAt()   { return lastSeenAt; }
}
