package app.bloom.identity.repository;

import app.bloom.identity.model.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshSession s SET s.revoked = true WHERE s.familyId = :familyId")
    void revokeAllByFamilyId(UUID familyId);

    @Modifying
    @Query("UPDATE RefreshSession s SET s.revoked = true WHERE s.accountId = :accountId")
    void revokeAllByAccountId(UUID accountId);
}
