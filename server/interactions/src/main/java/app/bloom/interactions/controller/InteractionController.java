package app.bloom.interactions.controller;

import app.bloom.interactions.dto.InteractionResponse;
import app.bloom.interactions.model.Reaction;
import app.bloom.interactions.service.InteractionService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interactions")
public class InteractionController {
    private final InteractionService service;

    public InteractionController(InteractionService service) {
        this.service = service;
    }

    @PostMapping("/{userId}/like")
    public InteractionResponse like(@AuthenticationPrincipal UUID actor, @PathVariable UUID userId,
            @RequestHeader("Idempotency-Key") UUID requestId) {
        return service.react(actor, userId, Reaction.LIKE, requestId);
    }

    @PostMapping("/{userId}/skip")
    public InteractionResponse skip(@AuthenticationPrincipal UUID actor, @PathVariable UUID userId,
            @RequestHeader("Idempotency-Key") UUID requestId) {
        return service.react(actor, userId, Reaction.SKIP, requestId);
    }

    @PostMapping("/{userId}/super-interest")
    public InteractionResponse superInterest(@AuthenticationPrincipal UUID actor, @PathVariable UUID userId,
            @RequestHeader("Idempotency-Key") UUID requestId) {
        return service.react(actor, userId, Reaction.SUPER_INTEREST, requestId);
    }
}
