package app.bloom.identity.service;

import app.bloom.identity.model.AccountStatus;
import app.bloom.identity.repository.AccountRepository;
import app.bloom.identity.repository.EventRepository;
import app.bloom.identity.repository.SecurityAudit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionService {
    private final AccountRepository accounts;
    private final SecurityAudit audit;
    private final EventRepository events;

    public AccountDeletionService(AccountRepository accounts, SecurityAudit audit, EventRepository events) {
        this.accounts = accounts;
        this.audit = audit;
        this.events = events;
    }

    @Transactional
    public void delete(UUID id) {
        accounts.lockById(id).ifPresent(account -> {
            account.changeStatus(AccountStatus.DELETED);
            events.append(account, "account.deleted", null, "ACCOUNT_DELETED");
            // The database cascades deletion to every refresh session.
            accounts.delete(account);
            accounts.flush();
            audit.record(id, "ACCOUNT_DELETED", id);
        });
    }
}
