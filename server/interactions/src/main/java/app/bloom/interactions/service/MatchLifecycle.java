package app.bloom.interactions.service;

import app.bloom.interactions.model.MatchStatus;
import app.bloom.interactions.model.Pair;
import app.bloom.interactions.repository.EventRepository;
import app.bloom.interactions.repository.MatchRepository;
import app.bloom.interactions.repository.PairRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchLifecycle {
    private final MatchRepository matches;
    private final PairRepository pairs;
    private final EventRepository events;
    private final int cooldownDays;

    public MatchLifecycle(MatchRepository matches, PairRepository pairs, EventRepository events,
            @Value("${bloom.matching.cooldown-days}") int cooldownDays) {
        this.matches = matches;
        this.pairs = pairs;
        this.events = events;
        this.cooldownDays = cooldownDays;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void close(Pair locked, MatchStatus status, String reason) {
        if (locked.matchId() != null) {
            matches.close(locked.matchId(), status).ifPresent(match -> events.append(match, "match.closed", reason));
        }
        pairs.close(locked, Instant.now().plus(cooldownDays, ChronoUnit.DAYS));
    }
}
