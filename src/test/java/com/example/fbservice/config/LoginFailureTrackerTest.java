package com.example.fbservice.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class LoginFailureTrackerTest {
    @Test
    void locksAtTwentyFailuresForFifteenMinutesAndThenAutomaticallyRecovers() {
        AtomicLong now=new AtomicLong(1_000_000L);
        LoginFailureTracker tracker=new LoginFailureTracker(now::get);
        for(int i=0;i<19;i++) {
            tracker.recordFailure("valid-locked-user");
            assertThat(tracker.isTemporarilyLocked("valid-locked-user")).isFalse();
        }
        tracker.recordFailure("valid-locked-user");
        assertThat(tracker.isTemporarilyLocked("valid-locked-user")).isTrue();
        assertThat(tracker.isTemporarilyLocked("unrelated-user")).isFalse();
        now.addAndGet(Duration.ofMinutes(14).toMillis());
        tracker.recordFailure("valid-locked-user");
        assertThat(tracker.isTemporarilyLocked("valid-locked-user")).isTrue();
        now.addAndGet(Duration.ofMinutes(1).toMillis());
        assertThat(tracker.isTemporarilyLocked("valid-locked-user")).isFalse();
        tracker.recordFailure("valid-locked-user");
        assertThat(tracker.isTemporarilyLocked("valid-locked-user")).isFalse();
    }

    @Test
    void successfulLoginClearsPriorFailuresAndInvalidNamesAreNotTracked() {
        LoginFailureTracker tracker=new LoginFailureTracker(()->4_000_000L);
        for(int i=0;i<19;i++) tracker.recordFailure("owner-with-reset");
        tracker.resetAfterSuccess("owner-with-reset");
        tracker.recordFailure("owner-with-reset");
        assertThat(tracker.isTemporarilyLocked("owner-with-reset")).isFalse();
        for(int i=0;i<30;i++) tracker.recordFailure("@invalid name");
        assertThat(tracker.isTemporarilyLocked("@invalid name")).isFalse();
    }

    @Test
    void concurrentFailuresCannotRacePastTheThreshold() throws Exception {
        LoginFailureTracker tracker=new LoginFailureTracker(()->3_000_000L);
        var pool=Executors.newFixedThreadPool(6);
        try {
            var futures=new ArrayList<java.util.concurrent.Future<?>>();
            for(int i=0;i<6;i++) futures.add(pool.submit(()->{
                for(int k=0;k<5;k++) tracker.recordFailure("parallel-owner");
            }));
            for(var f:futures) f.get(10,TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertThat(tracker.isTemporarilyLocked("parallel-owner")).isTrue();
    }
}
