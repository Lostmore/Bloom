package app.bloom.identity.service;

import app.bloom.identity.repository.AccountRepository;
import app.bloom.identity.repository.SecurityAudit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionService {
    private final AccountRepository accounts;
    private final SecurityAudit audit;

    public AccountDeletionService(AccountRepository accounts, SecurityAudit audit) {
        this.accounts = accounts;
        this.audit = audit;
    }

    @Transactional
    public void delete(UUID id) {
        accounts.lockById(id).ifPresent(account -> {
            // The database cascades deletion to every refresh session.
            accounts.delete(account);
            accounts.flush();
            audit.record(id, "ACCOUNT_DELETED", id);
        });
    }
}
