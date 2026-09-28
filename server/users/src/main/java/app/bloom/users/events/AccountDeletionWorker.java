package app.bloom.users.events;

import app.bloom.users.client.IdentityClient;
import app.bloom.users.repository.DeletionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AccountDeletionWorker {
    private final DeletionRepository deletions;
    private final IdentityClient identity;
    private final TransactionTemplate transactions;

    public AccountDeletionWorker(DeletionRepository deletions, IdentityClient identity, TransactionTemplate transactions) {
        this.deletions = deletions;
        this.identity = identity;
        this.transactions = transactions;
    }

    public boolean processNext() {
        return Boolean.TRUE.equals(transactions.execute(status -> {
            var next = deletions.lockNext();
            if (next.isEmpty()) {
                return false;
            }
            try {
                identity.delete(next.get());
                deletions.complete(next.get());
            } catch (ResponseStatusException exception) {
                deletions.retry(next.get());
            }
            return true;
        }));
    }
}
