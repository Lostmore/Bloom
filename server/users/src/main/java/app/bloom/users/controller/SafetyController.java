package app.bloom.users.controller;

import app.bloom.users.dto.CreateReportRequest;
import app.bloom.users.service.SafetyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SafetyController {
    private final SafetyService safety;

    public SafetyController(SafetyService safety) {
        this.safety = safety;
    }

    @PostMapping("/users/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@AuthenticationPrincipal UUID user, @PathVariable UUID id) {
        safety.block(user, id);
    }

    @DeleteMapping("/users/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@AuthenticationPrincipal UUID user, @PathVariable UUID id) {
        safety.unblock(user, id);
    }

    @GetMapping("/users/me/blocks")
    public List<UUID> blocks(@AuthenticationPrincipal UUID user,
            @RequestParam(defaultValue = "00000000-0000-0000-0000-000000000000") UUID after,
            @RequestParam(defaultValue = "50") int limit) {
        return safety.blocks(user, after, limit);
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> report(@AuthenticationPrincipal UUID user, @Valid @RequestBody CreateReportRequest request) {
        return Map.of("id", safety.report(user, request));
    }
}
