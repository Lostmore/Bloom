package app.bloom.identity.repository;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SecurityAudit {
    private final JdbcClient jdbc;

    public SecurityAudit(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void record(UUID accountId, String action, UUID reference) {
        jdbc.sql("""
                INSERT INTO security_audit (id, account_id, action, reference_id)
                VALUES (:id, :account, :action, :reference)
                """)
                .param("id", UUID.randomUUID())
                .param("account", accountId)
                .param("action", action)
                .param("reference", reference)
                .update();
    }
}
