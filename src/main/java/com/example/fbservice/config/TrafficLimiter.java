package com.example.fbservice.config;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Per-instance bounded fixed-window limiter. Every key retains its own TTL;
 * a short login window must never expire an unrelated one-hour password-change limit.
 * All capacity checks and updates are atomic across concurrent servlet requests.
 */
@Component
public class TrafficLimiter {
    private static final int MAX_ENTRIES = 20000;
    private record Window(long startMillis, int hits, long durationMillis) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final LongSupplier nowMillis;

    public TrafficLimiter() {
        this(System::currentTimeMillis);
    }

    // Deterministic clock seam for security boundary and concurrency regressions.
    TrafficLimiter(LongSupplier nowMillis) {
        this.nowMillis = Objects.requireNonNull(nowMillis);
    }

    public synchronized void check(String key, int maxHits, Duration period) {
        if (key == null || key.isBlank() || maxHits < 1 || period == null
                || period.isZero() || period.isNegative())
            throw new IllegalArgumentException("Invalid limiter configuration");
        long windowMillis = period.toMillis();
        if (windowMillis < 1)
            throw new IllegalArgumentException("Traffic window must be at least one millisecond");
        long now = nowMillis.getAsLong();
        if (windows.size() >= MAX_ENTRIES) {
            // Expire each entry using its OWN duration; previously the caller's
            // duration could erase a longer, still-active security limit.
            windows.entrySet().removeIf(e -> now - e.getValue().startMillis()
                    >= e.getValue().durationMillis());
            if (windows.size() >= MAX_ENTRIES && !windows.containsKey(key))
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Traffic limit at capacity");
        }
        Window current = windows.get(key);
        if (current == null || now - current.startMillis() >= current.durationMillis()) {
            windows.put(key, new Window(now, 1, windowMillis));
        } else {
            if (current.durationMillis() != windowMillis)
                throw new IllegalArgumentException("Inconsistent duration for the same traffic key");
            if (current.hits() >= maxHits)
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Rate limit exceeded");
            windows.put(key, new Window(current.startMillis(), current.hits() + 1, windowMillis));
        }
    }
}
