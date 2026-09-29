package app.bloom.interactions.controller;

import app.bloom.interactions.dto.MatchPage;
import app.bloom.interactions.model.Match;
import app.bloom.interactions.service.MatchService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/matches")
public class MatchController {
    private final MatchService service;

    public MatchController(MatchService service) {
        this.service = service;
    }

    @GetMapping
    public MatchPage list(@AuthenticationPrincipal UUID user,
            @RequestParam(required = false) UUID after, @RequestParam(defaultValue = "20") int limit) {
        return service.list(user, after, limit);
    }

    @GetMapping("/{id}")
    public Match get(@AuthenticationPrincipal UUID user, @PathVariable UUID id) {
        return service.view(user, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unmatch(@AuthenticationPrincipal UUID user, @PathVariable UUID id) {
        service.unmatch(user, id);
    }
}
