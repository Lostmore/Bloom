package app.bloom.identity.service;

import app.bloom.identity.model.Account;
import app.bloom.identity.model.AccountStatus;
import app.bloom.identity.repository.AccountRepository;
import app.bloom.identity.repository.EventRepository;
import app.bloom.identity.repository.RefreshSessionRepository;
import app.bloom.identity.repository.SecurityAudit;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountStatusService {
    private final AccountRepository accounts;
    private final RefreshSessionRepository sessions;
    private final EventRepository events;
    private final SecurityAudit audit;

    public AccountStatusService(AccountRepository accounts, RefreshSessionRepository sessions,
            EventRepository events, SecurityAudit audit) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.events = events;
        this.audit = audit;
    }

    @Transactional
    public void update(UUID id, AccountStatus status) {
        if (status != AccountStatus.ACTIVE && status != AccountStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected ACTIVE or SUSPENDED");
        }
        Account account = accounts.lockById(id)
                .filter(found -> found.getStatus() != AccountStatus.DELETED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account unavailable"));
        if (account.getStatus() == status) {
            return;
        }

        account.changeStatus(status);
        accounts.saveAndFlush(account);
        sessions.revokeAllByAccountId(id);
        boolean blocked = status == AccountStatus.SUSPENDED;
        String action = blocked ? "ACCOUNT_BLOCKED" : "ACCOUNT_UNBLOCKED";
        events.append(account, blocked ? "account.blocked" : "account.unblocked", null, action);
        audit.record(id, action, id);
    }
}
