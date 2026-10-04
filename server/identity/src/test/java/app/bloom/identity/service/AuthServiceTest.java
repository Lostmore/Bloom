package app.bloom.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bloom.identity.security.JwtProvider;
import app.bloom.identity.model.AccessStatus;
import io.jsonwebtoken.Jwts;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private static final String PRIVATE_KEY = """
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

    private static final String PUBLIC_KEY = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA45pbpt/Nd9d2STaLgz/u
            tgIw555kTzAiL8wsCnCFK77tEqlUc/HuGrXx8VRCLZF11H5FgxpL0bWkTdUeycw2
            f9waz0f7R+y53s3liVmojfrbfiyvP0/H9eNw6GFDwFGyv2bkI3mr3ukEGhMiii1R
            INCtPQNiqQmT7iKr6nLRpAiFWEulxo5z/CazR6Dg2w9oUsUFdxM5U4hFC+WsIuX9
            iSsANOvHdckqeI6QLGJWhXOkwz1zh9xQ7zZCvmkQDYNWTAv2S1Nr82Tk9P7Blt0I
            GucDiu+3wnNbggTEDUiBB1tFKgSZBrqvhN0sBkP/1biEIucRmAIMfYPz7E0rq5KT
            LQIDAQAB
            -----END PUBLIC KEY-----""";

    @Test
    void jwtRoundTripWithRsaKeys() {
        var provider = new JwtProvider(PRIVATE_KEY, PUBLIC_KEY, Duration.ofMinutes(15), Duration.ofDays(30));

        String token = provider.accessToken(UUID.randomUUID(), UUID.randomUUID(), 0, AccessStatus.ONBOARDING);
        assertThat(provider.parse(token)).isNotNull();
        assertThat(provider.parse("bad-token")).isNull();
    }

    @Test
    void jwtCannotBeVerifiedWithDifferentKey() throws Exception {
        var issuer = new JwtProvider(PRIVATE_KEY, PUBLIC_KEY, Duration.ofMinutes(15), Duration.ofDays(30));

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp2 = gen.generateKeyPair();
        String otherPrivate = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(kp2.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
        String otherPublic = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(kp2.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";

        var other = new JwtProvider(otherPrivate, otherPublic, Duration.ofMinutes(15), Duration.ofDays(30));

        String token = issuer.accessToken(UUID.randomUUID(), UUID.randomUUID(), 0, AccessStatus.ONBOARDING);
        assertThat(issuer.parse(token)).isNotNull();
        assertThat(other.parse(token)).isNull();
    }

    @Test
    void accessStatusesRoundTripAndUnknownStatusesAreRejected() {
        var provider = new JwtProvider(PRIVATE_KEY, PUBLIC_KEY, Duration.ofMinutes(15), Duration.ofDays(30));
        for (AccessStatus status : AccessStatus.values()) {
            var claims = provider.parse(provider.accessToken(UUID.randomUUID(), UUID.randomUUID(), 0, status));
            assertThat(claims.get("status")).isEqualTo(status.name());
            assertThat(provider.accessStatus(claims)).isEqualTo(status);
        }
        assertThat(provider.accessStatus(Jwts.claims().build())).isEqualTo(AccessStatus.ONBOARDING);
        assertThatThrownBy(() -> provider.accessStatus(Jwts.claims().add("status", "UNKNOWN").build()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.accessStatus(Jwts.claims().add("status", 123).build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void jwtRejectsInvalidLifetimes() {
        assertThatThrownBy(() -> new JwtProvider(PRIVATE_KEY, PUBLIC_KEY,
                Duration.ofHours(1), Duration.ofDays(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
