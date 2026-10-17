package com.example.fbservice.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Per-instance bounded fixed-window limit, fail-closed if capacity is exhausted. */
@Component
public class TrafficLimiter {
    private static final int MAX_ENTRIES = 20000;
    private record Window(long startMillis, int hits) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public void check(String key, int maxHits, Duration period) {
        if (key == null || key.isBlank() || maxHits < 1 || period == null || period.isZero() || period.isNegative())
            throw new IllegalArgumentException("Invalid limiter configuration");
        long now = System.currentTimeMillis();
        long windowMillis = period.toMillis();
        AtomicBoolean allowed = new AtomicBoolean(true);
        if (windows.size() >= MAX_ENTRIES) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startMillis() >= windowMillis);
            if (windows.size() >= MAX_ENTRIES && !windows.containsKey(key)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Traffic limit at capacity");
            }
        }
        windows.compute(key, (k, current) -> {
            if (current == null || now - current.startMillis() >= windowMillis) {
                return new Window(now, 1);
            }
            if (current.hits() >= maxHits) {
                allowed.set(false);
                return current;
            }
            return new Window(current.startMillis(), current.hits() + 1);
        });
        if (!allowed.get()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
    }
}
