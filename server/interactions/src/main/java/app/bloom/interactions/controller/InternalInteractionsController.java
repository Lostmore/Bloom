package app.bloom.interactions.controller;

import app.bloom.interactions.dto.AccessResponse;
import app.bloom.interactions.dto.ExclusionsRequest;
import app.bloom.interactions.dto.MatchAccessRequest;
import app.bloom.interactions.repository.PairRepository;
import app.bloom.interactions.service.MatchService;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/interactions")
public class InternalInteractionsController {
    private final MatchService matches;
    private final PairRepository pairs;

    public InternalInteractionsController(MatchService matches, PairRepository pairs) {
        this.matches = matches;
        this.pairs = pairs;
    }

    @PostMapping("/can-access-match")
    public AccessResponse canAccess(@Valid @RequestBody MatchAccessRequest request) {
        return new AccessResponse(matches.canAccess(request.userId(), request.matchId()));
    }

    @PostMapping("/exclusions")
    public Set<UUID> exclusions(@Valid @RequestBody ExclusionsRequest request) {
        return pairs.excluded(request.userId(), request.candidateIds());
    }
}
