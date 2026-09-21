package app.bloom.identity.security;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class Argon2IdPasswordEncoder implements PasswordEncoder {

    private static final int SALT_LEN = 16;
    private static final int HASH_LEN = 32;

    private final int memory;
    private final int iterations;
    private final int parallelism;
    private final SecureRandom rng = new SecureRandom();

    public Argon2IdPasswordEncoder(int memory, int iterations, int parallelism) {
        this.memory = memory;
        this.iterations = iterations;
        this.parallelism = parallelism;
    }

    @Override
    public String encode(CharSequence raw) {
        byte[] salt = new byte[SALT_LEN];
        rng.nextBytes(salt);
        byte[] hash = hash(raw, salt, memory, iterations, parallelism);
        return String.format("$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s", memory, iterations, parallelism,
                Base64.getEncoder().withoutPadding().encodeToString(salt),
                Base64.getEncoder().withoutPadding().encodeToString(hash));
    }

    @Override
    public boolean matches(CharSequence raw, String encoded) {
        if (encoded == null || !encoded.startsWith("$argon2id$")) return false;
        try {
            String[] p = encoded.split("\\$");
            if (p.length != 6) return false;

            int m = 0, t = 0, par = 0;
            for (String kv : p[3].split(",")) {
                String[] pair = kv.split("=");
                switch (pair[0]) {
                    case "m" -> m = Integer.parseInt(pair[1]);
                    case "t" -> t = Integer.parseInt(pair[1]);
                    case "p" -> par = Integer.parseInt(pair[1]);
                }
            }
            byte[] salt = Base64.getDecoder().decode(p[4]);
            byte[] expected = Base64.getDecoder().decode(p[5]);
            byte[] computed = hash(raw, salt, m, t, par);

            // constant-time comparison
            int diff = expected.length ^ computed.length;
            for (int i = 0; i < expected.length && i < computed.length; i++)
                diff |= expected[i] ^ computed[i];
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] hash(CharSequence raw, byte[] salt, int m, int t, int p) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id).withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(m).withIterations(t).withParallelism(p)
                .withSalt(salt).build();
        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(params);
        byte[] out = new byte[HASH_LEN];
        gen.generateBytes(raw.toString().getBytes(StandardCharsets.UTF_8), out);
        return out;
    }
}
