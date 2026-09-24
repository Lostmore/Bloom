package app.bloom.identity.security;

import app.bloom.identity.exception.AuthenticationException;
import app.bloom.identity.model.Account;
import app.bloom.identity.repository.AccountRepository;
import app.bloom.identity.repository.RefreshSessionRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AccessValidator {
    private final JwtProvider jwt;
    private final AccountRepository accounts;
    private final RefreshSessionRepository sessions;

    public AccessValidator(JwtProvider jwt, AccountRepository accounts, RefreshSessionRepository sessions) {
        this.jwt = jwt;
        this.accounts = accounts;
        this.sessions = sessions;
    }

    public AccessIdentity validate(String token) {
        var claims = jwt.parse(token);
        if (claims == null || !"access".equals(jwt.type(claims))) {
            throw new AuthenticationException();
        }
        try {
            var accountId = jwt.accountId(claims);
            var familyId = jwt.familyId(claims);
            long version = jwt.tokenVersion(claims);
            accounts.findById(accountId).filter(Account::isActive)
                    .filter(account -> account.getTokenVersion() == version)
                    .orElseThrow(AuthenticationException::new);
            if (!sessions.existsByAccountIdAndFamilyIdAndRevokedFalseAndExpiresAtAfter(
                    accountId, familyId, Instant.now())) {
                throw new AuthenticationException();
            }
            return new AccessIdentity(accountId, familyId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AuthenticationException();
        }
    }
}
