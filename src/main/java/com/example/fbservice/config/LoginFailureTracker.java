package com.example.fbservice.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Component;

/**
 * Additional account-wide temporary login lock for distributed source-IP guessing.
 * Stored keys are SHA-256 digests, never plaintext usernames or passwords.
 *
 * Single-node defense in depth only. Use shared gateway/Redis storage if the service
 * is ever run behind multiple app replicas. Lockouts can be abused for account denial
 * of service, so the UI deliberately uses a generic invalid-credential response.
 */
@Component
public class LoginFailureTracker {
    private static final int THRESHOLD = 20;
    private static final long WINDOW_MILLIS = Duration.ofMinutes(15).toMillis();
    private static final long LOCK_MILLIS = Duration.ofMinutes(15).toMillis();
    private static final int MAX_ENTRIES = 20_000;
    private record Attempt(long start, int failures, long blockedUntil) {}

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public LoginFailureTracker() {
        this(System::currentTimeMillis);
    }

    LoginFailureTracker(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized boolean isTemporarilyLocked(String username) {
        String key=key(username);
        if (key == null) return false;
        long now=clock.getAsLong();
        Attempt current=attempts.get(key);
        if (current==null) return false;
        if (current.blockedUntil() > now) return true;
        if (current.blockedUntil() > 0 || now-current.start() >= WINDOW_MILLIS) {
            attempts.remove(key);
        }
        return false;
    }

    public synchronized void recordFailure(String username) {
        String key=key(username);
        if (key==null) return;
        long now=clock.getAsLong();
        Attempt current=attempts.get(key);
        if (current!=null && current.blockedUntil()>now) return;
        if (current==null || current.blockedUntil()>0 ||
                now-current.start() >= WINDOW_MILLIS) {
            if (current==null && attempts.size() >= MAX_ENTRIES) {
                // Preserve known-account lockouts instead of evicting them.
                // Existing per-peer rate limiting still applies for excess distinct names.
                attempts.entrySet().removeIf(e -> expired(e.getValue(),now));
                if (attempts.size() >= MAX_ENTRIES) return;
            }
            attempts.put(key,new Attempt(now,1,0));
        } else {
            int failures=current.failures()+1;
            attempts.put(key,new Attempt(current.start(),failures,
                failures >= THRESHOLD ? now+LOCK_MILLIS : 0));
        }
    }

    public synchronized void resetAfterSuccess(String username) {
        String key=key(username);
        if(key!=null) attempts.remove(key);
    }

    private static boolean expired(Attempt attempt, long now) {
        if(attempt.blockedUntil()>now) return false;
        if(attempt.blockedUntil()>0) return true;
        return now-attempt.start()>=WINDOW_MILLIS;
    }

    private static String key(String username) {
        if(username==null || !username.matches("[A-Za-z0-9._-]{3,40}"))return null;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(username.getBytes(StandardCharsets.UTF_8)));
        } catch(NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
