package com.example.fbservice.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Isolated, in-memory one-time challenge primitive for future email verification
 * and password reset. Deliberately NOT a Spring bean or HTTP endpoint.
 *
 * No mailer, reset route, or account write is enabled here. Password reset must
 * use persistent, shared storage and verified outbound delivery before launch.
 */
public final class AccountActionTokenStore {
    public enum Purpose { VERIFY_EMAIL, RESET_PASSWORD }
    private static final int TOKEN_BYTES = 32;
    private static final int MAX_ENTRIES = 5000;
    private static final long TTL_MILLIS = Duration.ofMinutes(15).toMillis();
    private record Challenge(long accountId, Purpose purpose, long expiresAtMillis) {}

    private final SecureRandom random;
    private final LongSupplier now;
    // Keys are SHA-256 digests of CSPRNG tokens. Never store raw bearer values.
    private final Map<String, Challenge> entries = new HashMap<>();

    public AccountActionTokenStore() {
        this(new SecureRandom(), System::currentTimeMillis);
    }

    AccountActionTokenStore(SecureRandom random, LongSupplier now) {
        this.random = Objects.requireNonNull(random);
        this.now = Objects.requireNonNull(now);
    }

    /** Only an outbound verified-email adapter may ever disclose token() to its recipient. */
    public static final class IssuedToken {
        private final String token;
        private final long expiresAtMillis;

        private IssuedToken(String token, long expiresAtMillis) {
            this.token = token;
            this.expiresAtMillis = expiresAtMillis;
        }

        public String token() { return token; }
        public long expiresAtMillis() { return expiresAtMillis; }

        @Override
        public String toString() { return "IssuedToken([REDACTED])"; }
    }

    public synchronized IssuedToken issue(long accountId, Purpose purpose) {
        if (accountId <= 0 || purpose == null)
            throw new IllegalArgumentException("Valid account and purpose required");
        long time = now.getAsLong();
        entries.entrySet().removeIf(e -> e.getValue().expiresAtMillis <= time);
        if (entries.size() >= MAX_ENTRIES)
            throw new IllegalStateException("Challenge store is at capacity");
        // Reissuing a challenge for the same purpose invalidates the old one.
        entries.entrySet().removeIf(e ->
            e.getValue().accountId == accountId && e.getValue().purpose == purpose);

        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long expiration = Math.addExact(time, TTL_MILLIS);
        entries.put(digest(raw), new Challenge(accountId, purpose, expiration));
        return new IssuedToken(raw, expiration);
    }

    /**
     * Consume atomically at most once. Invalid, expired, wrong-account or
     * wrong-purpose values never grant a valid action.
     */
    public synchronized boolean consume(long accountId, Purpose purpose, String raw) {
        if (accountId <= 0 || purpose == null || raw == null ||
                !raw.matches("[A-Za-z0-9_-]{43}"))
            return false;
        String key = digest(raw);
        Challenge challenge = entries.get(key);
        if (challenge == null || challenge.accountId != accountId
                || challenge.purpose != purpose)
            return false;
        entries.remove(key);
        return challenge.expiresAtMillis > now.getAsLong();
    }

    private static String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.US_ASCII));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
