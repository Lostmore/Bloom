package app.bloom.identity.service;

import app.bloom.identity.dto.AuthResponse;
import app.bloom.identity.dto.LoginRequest;
import app.bloom.identity.dto.LogoutRequest;
import app.bloom.identity.dto.RefreshRequest;
import app.bloom.identity.dto.RegisterRequest;
import app.bloom.identity.exception.AuthenticationException;
import app.bloom.identity.exception.InvalidTokenException;
import app.bloom.identity.exception.RegistrationException;
import app.bloom.identity.model.Account;
import app.bloom.identity.model.RefreshSession;
import app.bloom.identity.repository.AccountRepository;
import app.bloom.identity.repository.EventRepository;
import app.bloom.identity.repository.RefreshSessionRepository;
import app.bloom.identity.repository.SecurityAudit;
import app.bloom.identity.security.JwtProvider;
import app.bloom.identity.security.LoginProtection;
import app.bloom.identity.security.TokenHasher;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final AccountRepository accounts;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final JwtProvider jwt;
    private final LoginProtection protection;
    private final SecurityAudit audit;
    private final EventRepository events;
    private final String dummyHash;

    public AuthService(AccountRepository accounts, RefreshSessionRepository sessions, PasswordEncoder passwords, JwtProvider jwt,
            LoginProtection protection, SecurityAudit audit, EventRepository events) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwords = passwords;
        this.jwt = jwt;
        this.protection = protection;
        this.audit = audit;
        this.events = events;
        dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String device, String ip) {
        protection.require("register", request.phoneNumber(), 5, Duration.ofHours(1), false);
        if (accounts.existsByPhoneNumber(request.phoneNumber())) {
            throw new RegistrationException("Registration failed");
        }
        Account account = Account.create(request.phoneNumber(), passwords.encode(request.password()));
        accounts.saveAndFlush(account);
        audit.record(account.getId(), "REGISTER", account.getId());
        return issueTokens(account, device, ip);
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public AuthResponse login(LoginRequest request, String device, String ip) {
        protection.require("login", request.phoneNumber(), 8, Duration.ofMinutes(15), true);
        var found = accounts.findByPhoneNumber(request.phoneNumber());
        boolean valid = passwords.matches(request.password(), found.map(Account::getPasswordHash).orElse(dummyHash));
        if (!valid || found.isEmpty() || !found.get().isActive()) {
            audit.record(found.map(Account::getId).orElse(null), "LOGIN_FAILED", null);
            throw new AuthenticationException();
        }
        Account account = found.get();
        account.touchLastSeen();
        protection.loginSucceeded(request.phoneNumber());
        audit.record(account.getId(), "LOGIN", account.getId());
        return issueTokens(account, device, ip);
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponse refresh(RefreshRequest request, String device, String ip) {
        Claims claims = requireRefresh(request.refreshToken());
        Account account = accounts.lockById(jwt.accountId(claims)).filter(Account::isActive)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        RefreshSession session = sessions.findByTokenHash(TokenHasher.sha256(request.refreshToken()))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        if (!session.getAccountId().equals(account.getId()) || !session.getId().equals(jwt.sessionId(claims))
                || !session.getFamilyId().equals(jwt.familyId(claims))) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        if (session.isRevoked()) {
            sessions.revokeAllByFamilyId(session.getFamilyId());
            events.append(account, "session.revoked", session.getFamilyId(), "REFRESH_REUSE");
            audit.record(account.getId(), "REFRESH_REUSE", session.getFamilyId());
            throw new InvalidTokenException("Invalid refresh token");
        }
        if (session.isExpired()) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        session.revoke();
        sessions.saveAndFlush(session);
        RefreshSession next = RefreshSession.rotate(session, "", Instant.now().plus(jwt.getRefreshTtl()), device, ip);
        String refresh = jwt.refreshToken(next.getId(), account.getId(), next.getFamilyId());
        next.withTokenHash(TokenHasher.sha256(refresh));
        sessions.save(next);
        account.touchLastSeen();
        audit.record(account.getId(), "REFRESH", next.getFamilyId());
        return response(account, next, refresh);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        Claims claims;
        try {
            claims = requireRefresh(request.refreshToken());
        } catch (InvalidTokenException exception) {
            return;
        }
        var account = accounts.lockById(jwt.accountId(claims));
        if (account.isEmpty()) {
            return;
        }
        sessions.findByTokenHash(TokenHasher.sha256(request.refreshToken())).ifPresent(session -> {
            sessions.revokeAllByFamilyId(session.getFamilyId());
            events.append(account.get(), "session.revoked", session.getFamilyId(), "LOGOUT");
            audit.record(session.getAccountId(), "LOGOUT", session.getFamilyId());
        });
    }

    @Transactional
    public void logoutAll(UUID accountId) {
        Account account = accounts.lockById(accountId).filter(Account::isActive)
                .orElseThrow(AuthenticationException::new);
        account.incrementTokenVersion();
        accounts.saveAndFlush(account);
        sessions.revokeAllByAccountId(accountId);
        events.append(account, "sessions.revoked", null, "LOGOUT_ALL");
        audit.record(accountId, "LOGOUT_ALL", accountId);
    }

    public record SessionView(
            UUID id,
            String device,
            String ip,
            Instant createdAt,
            Instant expiresAt
    ) {
    }

    @Transactional(readOnly = true)
    public List<SessionView> sessions(UUID accountId) {
        return sessions.findByAccountIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(accountId, Instant.now())
                .stream()
                .map(session -> new SessionView(session.getFamilyId(),
                    session.getDeviceInfo(), session.getIpAddress(),
                    session.getCreatedAt(), session.getExpiresAt()))
                .toList();
    }

    @Transactional
    public void revokeSession(UUID accountId, UUID familyId) {
        Account account = accounts.lockById(accountId).filter(Account::isActive)
                .orElseThrow(AuthenticationException::new);
        if (!sessions.existsByAccountIdAndFamilyIdAndRevokedFalseAndExpiresAtAfter(accountId, familyId, Instant.now())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session unavailable");
        }
        sessions.revokeAllByFamilyId(familyId);
        events.append(account, "session.revoked", familyId, "SESSION_REVOKED");
        audit.record(accountId, "SESSION_REVOKED", familyId);
    }

    private Claims requireRefresh(String token) {
        Claims claims = jwt.parse(token);
        try {
            if (claims == null || !"refresh".equals(jwt.type(claims))) {
                throw new InvalidTokenException("Invalid refresh token");
            }
            jwt.accountId(claims);
            jwt.sessionId(claims);
            jwt.familyId(claims);
            return claims;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidTokenException("Invalid refresh token");
        }
    }

    private AuthResponse issueTokens(Account account, String device, String ip) {
        RefreshSession session = RefreshSession.create(account.getId(), "", Instant.now().plus(jwt.getRefreshTtl()), device, ip);
        String refresh = jwt.refreshToken(session.getId(), account.getId(), session.getFamilyId());
        session.withTokenHash(TokenHasher.sha256(refresh));
        sessions.save(session);
        return response(account, session, refresh);
    }

    private AuthResponse response(Account account, RefreshSession session, String refresh) {
        return new AuthResponse(jwt.accessToken(account.getId(),
            session.getFamilyId(), account.getTokenVersion()),
            refresh, jwt.getAccessTtlSeconds());
    }
}
