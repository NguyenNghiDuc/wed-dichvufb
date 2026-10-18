package com.example.fbservice.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;

class TrafficLimiterTtlIsolationTest {
    @Test
    void cleanupOfShortWindowsCannotClearActiveHourlyPasswordThrottle() {
        AtomicLong time=new AtomicLong(1_000_000L);
        TrafficLimiter limiter=new TrafficLimiter(time::get);
        limiter.check("password-change:account-1", 2, Duration.ofHours(1));
        limiter.check("password-change:account-1", 2, Duration.ofHours(1));
        for(int i=0;i<19_999;i++)
            limiter.check("login:peer:"+i,1,Duration.ofMinutes(1));

        // The map is full. A later short-window login cleanup must NOT drop
        // the 1-hour password-change throttle after only two minutes.
        time.addAndGet(Duration.ofMinutes(2).toMillis());
        limiter.check("login:new-peer",1,Duration.ofMinutes(1));
        assertThatThrownBy(() -> limiter.check("password-change:account-1",
                2, Duration.ofHours(1)))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException)ex).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        time.addAndGet(Duration.ofHours(1).toMillis());
        limiter.check("password-change:account-1",2,Duration.ofHours(1));
    }

    @Test
    void concurrentNewKeysNeverOverflowTheConfiguredCapacity() throws Exception {
        AtomicLong time=new AtomicLong(2_000_000L);
        TrafficLimiter limiter=new TrafficLimiter(time::get);
        var executor=Executors.newFixedThreadPool(8);
        try {
            var tasks=new ArrayList<java.util.concurrent.Future<?>>();
            for(int worker=0;worker<8;worker++) {
                final int n=worker;
                tasks.add(executor.submit(() -> {
                    for(int i=0;i<2_520;i++) {
                        try {
                            limiter.check("peer:"+n+":"+i,1,Duration.ofHours(1));
                        } catch (ResponseStatusException ex) {
                            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                        }
                    }
                }));
            }
            for(var task:tasks) task.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        var entries=(java.util.Map<?,?>)ReflectionTestUtils.getField(limiter,"windows");
        assertThat(entries).hasSize(20_000);
    }

    @Test
    void invalidSubMillisecondAndInconsistentDurationsAreRejected() {
        TrafficLimiter limiter=new TrafficLimiter(() -> 5000L);
        assertThatThrownBy(() -> limiter.check("key",1,Duration.ofNanos(1)))
            .isInstanceOf(IllegalArgumentException.class);
        limiter.check("key",2,Duration.ofMinutes(10));
        assertThatThrownBy(() -> limiter.check("key",2,Duration.ofHours(1)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
