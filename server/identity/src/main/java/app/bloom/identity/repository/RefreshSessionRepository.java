package app.bloom.identity.repository;

import app.bloom.identity.model.RefreshSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);

    boolean existsByAccountIdAndFamilyIdAndRevokedFalseAndExpiresAtAfter(UUID accountId, UUID familyId, Instant now);

    List<RefreshSession> findByAccountIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(UUID accountId, Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshSession session SET session.revoked = true WHERE session.familyId = :familyId")
    void revokeAllByFamilyId(UUID familyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshSession session SET session.revoked = true WHERE session.accountId = :accountId")
    void revokeAllByAccountId(UUID accountId);
}
