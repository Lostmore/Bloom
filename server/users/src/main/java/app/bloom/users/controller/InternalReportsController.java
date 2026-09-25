package app.bloom.users.controller;

import app.bloom.users.model.UserReport;
import app.bloom.users.service.SafetyService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalReportsController {
    private final SafetyService safety;

    public InternalReportsController(SafetyService safety) {
        this.safety = safety;
    }

    @GetMapping("/internal/reports/{id}")
    public UserReport report(@PathVariable UUID id) {
        return safety.report(id);
    }
}
