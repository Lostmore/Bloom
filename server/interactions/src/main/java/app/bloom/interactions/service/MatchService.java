package app.bloom.interactions.service;

import app.bloom.interactions.client.UsersClient;
import app.bloom.interactions.dto.MatchPage;
import app.bloom.interactions.model.Match;
import app.bloom.interactions.model.MatchStatus;
import app.bloom.interactions.model.Pair;
import app.bloom.interactions.repository.MatchRepository;
import app.bloom.interactions.repository.PairRepository;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MatchService {
    private final MatchRepository matches;
    private final PairRepository pairs;
    private final MatchLifecycle lifecycle;
    private final UsersClient users;

    public MatchService(MatchRepository matches, PairRepository pairs, MatchLifecycle lifecycle, UsersClient users) {
        this.matches = matches;
        this.pairs = pairs;
        this.lifecycle = lifecycle;
        this.users = users;
    }

    public Match view(UUID user, UUID id) {
        Match match = member(user, id);
        if (match.status() != MatchStatus.ACTIVE || !users.canInteract(user, match.partner(user))) {
            throw InteractionService.unavailable();
        }
        return match;
    }

    public MatchPage list(UUID user, UUID after, int limit) {
        if (limit < 1 || limit > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and 50");
        }
        var rows = matches.page(user, after, limit + 1);
        boolean more = rows.size() > limit;
        var scanned = rows.subList(0, Math.min(limit, rows.size()));
        Set<UUID> allowed = users.allowedTargets(user,
                scanned.stream().map(match -> match.partner(user)).collect(Collectors.toSet()));
        var visible = scanned.stream().filter(match -> allowed.contains(match.partner(user))).toList();
        UUID cursor = more ? scanned.getLast().id() : null;
        return new MatchPage(visible, cursor);
    }

    @Transactional(timeout = 15)
    public void unmatch(UUID user, UUID id) {
        Match initial = member(user, id);
        pairs.lockAccounts(initial.userA(), initial.userB());
        Pair pair = pairs.lock(initial.userA(), initial.userB());
        Match current = member(user, id);
        if (current.status() == MatchStatus.ACTIVE && id.equals(pair.matchId())) {
            lifecycle.close(pair, MatchStatus.UNMATCHED, "UNMATCHED");
        }
    }

    public boolean canAccess(UUID user, UUID id) {
        return matches.find(id).filter(match -> match.includes(user) && match.status() == MatchStatus.ACTIVE)
                .map(match -> users.canInteract(user, match.partner(user))).orElse(false);
    }

    private Match member(UUID user, UUID id) {
        return matches.find(id).filter(match -> match.includes(user)).orElseThrow(InteractionService::unavailable);
    }
}
