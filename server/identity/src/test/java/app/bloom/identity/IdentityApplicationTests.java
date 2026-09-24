package app.bloom.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bloom.identity.dto.AuthResponse;
import app.bloom.identity.dto.RefreshRequest;
import app.bloom.identity.exception.InvalidTokenException;
import app.bloom.identity.security.LoginProtection;
import app.bloom.identity.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
class IdentityApplicationTests {
    private static final EmbeddedPostgres POSTGRES = postgres();
    private static final AtomicLong PHONE = new AtomicLong(79000000000L);
    private static final String PASSWORD = "Strong-password-2026";
    private static final String INTERNAL_TOKEN = "test-internal-token-at-least-32-characters";

    @Autowired
    private MockMvc http;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private AuthService service;
    @Autowired
    private LoginProtection protection;

    private static EmbeddedPostgres postgres() {
        try {
            return EmbeddedPostgres.builder().setPort(0).start();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static final String TEST_PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDjmlum381313ZJ
            NouDP+62AjDnnmRPMCIvzCwKcIUrvu0SqVRz8e4atfHxVEItkXXUfkWDGkvRtaRN
            1R7JzDZ/3BrPR/tH7LnezeWJWaiN+tt+LK8/T8f143DoYUPAUbK/ZuQjeave6QQa
            EyKKLVEg0K09A2KpCZPuIqvqctGkCIVYS6XGjnP8JrNHoODbD2hSxQV3EzlTiEUL
            5awi5f2JKwA068d1ySp4jpAsYlaFc6TDPXOH3FDvNkK+aRANg1ZMC/ZLU2vzZOT0
            /sGW3Qga5wOK77fCc1uCBMQNSIEHW0UqBJkGuq+E3SwGQ//VuIQi5xGYAgx9g/Ps
            TSurkpMtAgMBAAECggEAZi5h8FCwr5lpTuQwjTuyIT9pkkq/cPfEmV3Z9dPfTaz5
            RTQFKMqYIaDGnpg737ZaxovsDDhNirg4LAM+v80xOITp+l4wF3HVXoxkfR3l+NwD
            F6nbAZsBREiYY5NrNdfX3mtEiBKzUS+eulAV5SAEOK3G4Mn4zLfylGo3cVmb+b5K
            yxhzlTHZ/IonBMLk4VpfaEy8fyQoy1YN3bj0Nr9o8uyckrLgpGTHRF95yjJRTgJQ
            o5zFnVh0oeemy7L9p1jVVXntTvXBoeSWkWRHKhs7S5kJ/16QQpGw91OGT7SeyWjL
            dCN4bbaLMOW8+aPC/YVStRMgXV6obpkIdWJh7Oi7twKBgQDsq/lWaZlO5zEP/WTb
            soebLI3mEug83Anu/fpdQlwiWLGYAzykmzhpohc6ste/ya+temzsK/yASDhgTGP8
            ti5A5P2YhiBtveDPX7+FVdOgF53DMx+fuIYWa2pWUzunUIH6Kvb/pHm/duVEVJbz
            YFg8mBPJ1BRQXJ/AluWnIpSaUwKBgQD2MMj7vPwc1Z3Gw5TNWECWUuogXwY9I9l1
            R3sldroJCi+M0mSkHX4s9Q1BoYv8Ouag9bfWkxN3ugxlUfRthJTDa7M5AYaaTyoA
            K6Imc2cBmwnlPMvYxiBHNPBRTtApiio+BQSokcp1nNu4R6v5FWqUSe6BeCB61LdL
            ij7wUGVsfwKBgAWazPzO42KK9w554bmSMMPr+jBIoldOnq2aNiOfPq9RimMfMIB1
            bJCT1hj9wz43eIGTNKEjKYegLjWZmYSSi+XdhmPmST5QENLYYaC2t5xC8ul8fHM7
            23EMeigxMGh1754NBmxnaLqwBh3oIusAjRZiQ5W0AAcnKmFVMXsTxpEhAoGBALuB
            nqI1ZyOjA19OuTfI/rA/iHiNR1grxMVRYxa+naSi8GJmICbrG52sKqORIehDCEMR
            wyoXrN0kg6pryCndr9xDXCaP5fADWW71zLNSc+LCLcA+FNgO1qjFRj/3Mg5hYBkK
            g2jMWZJInQR2+iFlUV6ODKzpTHuhJdXP/m3UHduLAoGBAN9E7Da8ALRCuv8CUfJg
            5m9s84Lv/kwv0DQW/3TFPajRKzEyuDhLM3Zl5adFO3DbFgsAfDr7sjw3hkLzebyc
            1iZi1xY+t978dS4a3O7cMI4z5Rzb2MQgUalTJU7ohquXMfDziRiwXGJDZnW8MyHt
            Jcq/JIog7jDTLYud3bCdqRUu
            -----END PRIVATE KEY-----""";

    private static final String TEST_PUBLIC_KEY = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA45pbpt/Nd9d2STaLgz/u
            tgIw555kTzAiL8wsCnCFK77tEqlUc/HuGrXx8VRCLZF11H5FgxpL0bWkTdUeycw2
            f9waz0f7R+y53s3liVmojfrbfiyvP0/H9eNw6GFDwFGyv2bkI3mr3ukEGhMiii1R
            INCtPQNiqQmT7iKr6nLRpAiFWEulxo5z/CazR6Dg2w9oUsUFdxM5U4hFC+WsIuX9
            iSsANOvHdckqeI6QLGJWhXOkwz1zh9xQ7zZCvmkQDYNWTAv2S1Nr82Tk9P7Blt0I
            GucDiu+3wnNbggTEDUiBB1tFKgSZBrqvhN0sBkP/1biEIucRmAIMfYPz7E0rq5KT
            LQIDAQAB
            -----END PUBLIC KEY-----""";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("bloom.jwt.private-key", () -> TEST_PRIVATE_KEY);
        registry.add("bloom.jwt.public-key", () -> TEST_PUBLIC_KEY);
        registry.add("bloom.internal-token", () -> INTERNAL_TOKEN);
        registry.add("bloom.security.argon2.memory", () -> 1024);
        registry.add("bloom.security.argon2.iterations", () -> 1);
    }

    @AfterAll
    static void closePostgres() throws Exception {
        POSTGRES.close();
    }

    @BeforeEach
    void resetLimits() {
        jdbc.sql("DELETE FROM auth_attempts").update();
    }

    @Test
    void registrationStoresHashesAndTokensContainNoPhone() throws Exception {
        Login account = register();
        String hash = jdbc.sql("SELECT password_hash FROM accounts WHERE phone_number = ?")
                .param(account.phone()).query(String.class).single();
        assertThat(hash).startsWith("$argon2id$").doesNotContain(PASSWORD);
        assertThat(jdbc.sql("SELECT count(*) FROM refresh_sessions WHERE token_hash = ?")
                .param(account.tokens().refreshToken()).query(Integer.class).single()).isZero();
        JsonNode payload = json.readTree(java.util.Base64.getUrlDecoder()
                .decode(account.tokens().accessToken().split("\\.")[1]));
        assertThat(payload.has("phone")).isFalse();
        assertThat(payload.path("fid").asText()).isNotBlank();
        http.perform(get("/auth/me").header("Authorization", bearer(account.tokens()))).andExpect(status().isOk());
    }

    @Test
    void reuseRevocationCommitsDespiteUnauthorizedResponse() throws Exception {
        Login account = register();
        AuthResponse rotated = refresh(account.tokens().refreshToken(), 200);
        refresh(account.tokens().refreshToken(), 401);
        refresh(rotated.refreshToken(), 401);
        http.perform(get("/auth/me").header("Authorization", bearer(rotated))).andExpect(status().isUnauthorized());
        int events = jdbc.sql("SELECT count(*) FROM security_audit WHERE action = 'REFRESH_REUSE'")
                .query(Integer.class).single();
        assertThat(events).isPositive();
    }

    @Test
    void logoutRevokesAccessAndEntireRotatedFamily() throws Exception {
        Login account = register();
        AuthResponse rotated = refresh(account.tokens().refreshToken(), 200);
        http.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", account.tokens().refreshToken()))))
                .andExpect(status().isNoContent());
        refresh(rotated.refreshToken(), 401);
        http.perform(get("/auth/me").header("Authorization", bearer(rotated))).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAllRevokesEverySession() throws Exception {
        Login account = register();
        AuthResponse second = login(account.phone(), PASSWORD, 200);
        http.perform(post("/auth/logout-all").header("Authorization", bearer(account.tokens())))
                .andExpect(status().isNoContent());
        refresh(second.refreshToken(), 401);
        http.perform(get("/auth/me").header("Authorization", bearer(second))).andExpect(status().isUnauthorized());
    }

    @Test
    void sessionRevocationIsOwnerScoped() throws Exception {
        Login first = register();
        Login other = register();
        String family = json.readTree(http.perform(get("/auth/me").header("Authorization", bearer(first.tokens())))
                .andReturn().getResponse().getContentAsString()).path("familyId").asText();
        http.perform(delete("/auth/sessions/" + family).header("Authorization", bearer(other.tokens())))
                .andExpect(status().isNotFound());
        http.perform(delete("/auth/sessions/" + family).header("Authorization", bearer(first.tokens())))
                .andExpect(status().isNoContent());
        refresh(first.tokens().refreshToken(), 401);
        http.perform(get("/auth/me").header("Authorization", bearer(other.tokens()))).andExpect(status().isOk());
    }

    @Test
    void simultaneousRefreshCannotCreateTwoLiveSessions() throws Exception {
        Login account = register();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> attempt = () -> {
                ready.countDown();
                start.await();
                try {
                    service.refresh(new RefreshRequest(account.tokens().refreshToken()), "test", "127.0.0.1");
                    return true;
                } catch (InvalidTokenException exception) {
                    return false;
                }
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isNotEqualTo(second.get(10, TimeUnit.SECONDS));
        }
        String id = json.readTree(java.util.Base64.getUrlDecoder()
                .decode(account.tokens().accessToken().split("\\.")[1])).path("sub").asText();
        assertThat(jdbc.sql("SELECT count(*) FROM refresh_sessions WHERE account_id = ? AND NOT revoked")
                .param(java.util.UUID.fromString(id)).query(Integer.class).single()).isZero();
    }

    @Test
    void suspendedAccountCannotLoginOrRefreshOrUseAccess() throws Exception {
        Login account = register();
        jdbc.sql("UPDATE accounts SET status = 'SUSPENDED' WHERE phone_number = ?").param(account.phone()).update();
        login(account.phone(), PASSWORD, 401);
        refresh(account.tokens().refreshToken(), 401);
        http.perform(get("/auth/me").header("Authorization", bearer(account.tokens()))).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidCredentialsAndTokenTypesAreRejected() throws Exception {
        Login account = register();
        login(account.phone(), "wrong-password", 401);
        login("+79999999999", "wrong-password", 401);
        http.perform(get("/auth/me").header("Authorization", "Bearer " + account.tokens().refreshToken()))
                .andExpect(status().isUnauthorized());
        http.perform(get("/auth/me").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        http.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("phoneNumber", "+79991234567", "password", "1234"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void internalApiRequiresServiceCredentialsAndRespectsLogout() throws Exception {
        Login account = register();
        String body = json.writeValueAsString(Map.of("token", account.tokens().accessToken()));
        http.perform(post("/internal/identity/introspect").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        var result = http.perform(post("/internal/identity/introspect")
                .header("X-Internal-Token", INTERNAL_TOKEN).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString()).path("active").asBoolean()).isTrue();
        http.perform(get("/unknown").header("Authorization", bearer(account.tokens()))).andExpect(status().isForbidden());
    }

    @Test
    void rateLimitPersistsAndBackoffDoesNotSleep() {
        protection.require("test", "key", 2, Duration.ofMinutes(15), false);
        protection.require("test", "key", 2, Duration.ofMinutes(15), false);
        assertThatThrownBy(() -> protection.require("test", "key", 2, Duration.ofMinutes(15), false))
                .isInstanceOf(ResponseStatusException.class);
        for (int attempt = 0; attempt < 3; attempt++) {
            protection.require("login", "phone", 8, Duration.ofMinutes(15), true);
        }
        assertThatThrownBy(() -> protection.require("login", "phone", 8, Duration.ofMinutes(15), true))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void freshRateLimitKeysNeverStartInBackoff() {
        for (int index = 0; index < 100; index++) {
            protection.require("fresh", "key-" + index, 5, Duration.ofMinutes(1), false);
        }
    }

    @Test
    void internalAccountDeletionRevokesAllTokensAndIsIdempotent() throws Exception {
        Login account = register();
        java.util.UUID id = jdbc.sql("SELECT id FROM accounts WHERE phone_number = ?")
                .param(account.phone()).query(java.util.UUID.class).single();
        String endpoint = "/internal/identity/accounts/" + id;
        http.perform(delete(endpoint).header("Authorization", bearer(account.tokens())))
                .andExpect(status().isUnauthorized());
        http.perform(delete(endpoint).header("X-Internal-Token", INTERNAL_TOKEN)).andExpect(status().isNoContent());
        http.perform(delete(endpoint).header("X-Internal-Token", INTERNAL_TOKEN)).andExpect(status().isNoContent());
        http.perform(get("/auth/me").header("Authorization", bearer(account.tokens())))
                .andExpect(status().isUnauthorized());
        refresh(account.tokens().refreshToken(), 401);
        assertThat(accountsRemaining(id)).isZero();
    }

    private int accountsRemaining(java.util.UUID id) {
        return jdbc.sql("SELECT count(*) FROM refresh_sessions WHERE account_id = ?")
                .param(id).query(Integer.class).single();
    }

    private Login register() throws Exception {
        String phone = "+" + PHONE.incrementAndGet();
        var response = http.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("phoneNumber", phone, "password", PASSWORD))))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return new Login(phone, json.readValue(response.getContentAsString(), AuthResponse.class));
    }

    private AuthResponse login(String phone, String password, int expected) throws Exception {
        var response = http.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("phoneNumber", phone, "password", password))))
                .andExpect(status().is(expected)).andReturn().getResponse();
        return expected == 200 ? json.readValue(response.getContentAsString(), AuthResponse.class) : null;
    }

    private AuthResponse refresh(String token, int expected) throws Exception {
        var response = http.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", token))))
                .andExpect(status().is(expected)).andReturn().getResponse();
        return expected == 200 ? json.readValue(response.getContentAsString(), AuthResponse.class) : null;
    }

    private String bearer(AuthResponse response) {
        return "Bearer " + response.accessToken();
    }

    private record Login(
            String phone,
            AuthResponse tokens
    ) {
    }
}
