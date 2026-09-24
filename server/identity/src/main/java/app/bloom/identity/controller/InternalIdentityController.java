package app.bloom.identity.controller;

import app.bloom.identity.exception.AuthenticationException;
import app.bloom.identity.model.Account;
import app.bloom.identity.model.AccountStatus;
import app.bloom.identity.repository.AccountRepository;
import app.bloom.identity.security.AccessIdentity;
import app.bloom.identity.security.AccessValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalIdentityController {
    private final AccessValidator validator;
    private final AccountRepository accounts;

    public InternalIdentityController(AccessValidator validator, AccountRepository accounts) {
        this.validator = validator;
        this.accounts = accounts;
    }

    public record TokenRequest(@NotBlank @Size(max = 4096) String token) {
    }

    public record TokenStatus(boolean active, UUID accountId, UUID familyId) {
    }

    public record AccountsRequest(@NotNull @Size(max = 200) Set<@NotNull UUID> userIds) {
    }

    @PostMapping("/internal/identity/introspect")
    public TokenStatus introspect(@Valid @RequestBody TokenRequest request) {
        try {
            AccessIdentity identity = validator.validate(request.token());
            return new TokenStatus(true, identity.accountId(), identity.familyId());
        } catch (AuthenticationException exception) {
            return new TokenStatus(false, null, null);
        }
    }

    @PostMapping("/internal/identity/active-accounts")
    public Set<UUID> activeAccounts(@Valid @RequestBody AccountsRequest request) {
        return accounts.findAllById(request.userIds()).stream()
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .map(Account::getId)
                .collect(Collectors.toSet());
    }
}
