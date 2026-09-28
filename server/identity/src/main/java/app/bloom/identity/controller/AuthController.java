package app.bloom.identity.controller;

import app.bloom.identity.dto.AuthResponse;
import app.bloom.identity.dto.LoginRequest;
import app.bloom.identity.dto.LogoutRequest;
import app.bloom.identity.dto.RefreshRequest;
import app.bloom.identity.dto.RegisterRequest;
import app.bloom.identity.security.AccessIdentity;
import app.bloom.identity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @ApiResponse(responseCode = "201", description = "Account created")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.register(request, device(http), http.getRemoteAddr()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return service.login(request, device(http), http.getRemoteAddr());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        return service.refresh(request, device(http), http.getRemoteAddr());
    }

    @ApiResponse(responseCode = "204", description = "Session revoked")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        service.logout(request);
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = "204", description = "Sessions revoked")
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AccessIdentity identity) {
        service.logoutAll(identity.accountId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AccessIdentity me(@AuthenticationPrincipal AccessIdentity identity) {
        return identity;
    }

    @GetMapping("/sessions")
    public List<AuthService.SessionView> sessions(@AuthenticationPrincipal AccessIdentity identity) {
        return service.sessions(identity.accountId());
    }

    @ApiResponse(responseCode = "204", description = "Session revoked")
    @DeleteMapping("/sessions/{familyId}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal AccessIdentity identity, @PathVariable UUID familyId) {
        service.revokeSession(identity.accountId(), familyId);
        return ResponseEntity.noContent().build();
    }

    private String device(HttpServletRequest request) {
        String agent = request.getHeader("User-Agent");
        return agent == null ? null : agent.substring(0, Math.min(agent.length(), 255));
    }
}
