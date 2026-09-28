package app.bloom.identity.controller;

import app.bloom.identity.dto.UpdateAccountStatusRequest;
import app.bloom.identity.service.AccountStatusService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccountStatusController {
    private final AccountStatusService accounts;
    private final byte[] moderationToken;

    public AccountStatusController(AccountStatusService accounts,
            @Value("${bloom.moderation-token:}") String moderationToken) {
        this.accounts = accounts;
        this.moderationToken = moderationToken.getBytes(StandardCharsets.UTF_8);
    }

    @PutMapping("/internal/identity/accounts/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable UUID id, @Valid @RequestBody UpdateAccountStatusRequest request,
            @RequestHeader(value = "X-Moderation-Token", defaultValue = "") String supplied) {
        // Knowing the introspection credential must not grant permission to block accounts.
        if (moderationToken.length < 32
                || !MessageDigest.isEqual(moderationToken, supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderation unavailable");
        }
        accounts.update(id, request.status());
    }
}
