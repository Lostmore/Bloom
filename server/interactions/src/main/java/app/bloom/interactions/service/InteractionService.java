package app.bloom.interactions.service;

import app.bloom.interactions.client.UsersClient;
import app.bloom.interactions.dto.InteractionResponse;
import app.bloom.interactions.model.Match;
import app.bloom.interactions.model.MatchStatus;
import app.bloom.interactions.model.Pair;
import app.bloom.interactions.model.Reaction;
import app.bloom.interactions.repository.EventRepository;
import app.bloom.interactions.repository.MatchRepository;
import app.bloom.interactions.repository.PairRepository;
import app.bloom.interactions.repository.RequestRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InteractionService {
    private final PairRepository pairs;
    private final MatchRepository matches;
    private final RequestRepository requests;
    private final EventRepository events;
    private final UsersClient users;
    private final int dailyLimit;

    public InteractionService(PairRepository pairs, MatchRepository matches, RequestRepository requests,
            EventRepository events, UsersClient users, @Value("${bloom.matching.daily-limit}") int dailyLimit) {
        this.pairs = pairs;
        this.matches = matches;
        this.requests = requests;
        this.events = events;
        this.users = users;
        this.dailyLimit = dailyLimit;
    }

    @Transactional(timeout = 15)
    public InteractionResponse react(UUID actor, UUID target, Reaction reaction, UUID requestId) {
        if (actor.equals(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot target yourself");
        }
        if (!pairs.lockAccounts(actor, target) || !users.canInteract(actor, target)) {
            throw unavailable();
        }
        var previous = requests.previous(actor, requestId, target, reaction);
        if (previous.isPresent()) {
            return previous.get();
        }
        if (requests.dailyCount(actor) >= dailyLimit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Daily interaction limit reached");
        }

        Pair pair = pairs.lock(actor, target);
        if (pair.cooldownUntil() != null) {
            if (pair.cooldownUntil().isAfter(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Pair is in cooldown");
            }
            pair = pairs.restart(pair);
        }
        UUID matchId = pair.matchId();
        if (matchId != null) {
            Match match = matches.find(matchId).orElseThrow(InteractionService::unavailable);
            if (match.status() != MatchStatus.ACTIVE) {
                throw unavailable();
            }
            if (reaction == Reaction.SKIP) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Use unmatch for an active match");
            }
        }
        pairs.react(pair, actor, reaction);
        Reaction opposite = pair.oppositeReaction(actor);
        if (matchId == null && reaction.positive() && opposite != null && opposite.positive()) {
            Match match = matches.create(pair);
            pairs.match(pair, match.id());
            events.append(match, "match.created", "MUTUAL_INTEREST");
            matchId = match.id();
        }
        var response = new InteractionResponse(target, reaction, matchId);
        requests.save(actor, requestId, reaction, response);
        return response;
    }

    static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource unavailable");
    }
}
