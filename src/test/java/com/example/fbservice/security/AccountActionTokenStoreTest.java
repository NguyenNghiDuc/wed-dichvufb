package com.example.fbservice.security;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AccountActionTokenStoreTest {
    @Test
    void oneTimeScopedChallengeIsNotExposedInStoreOrLogs() {
        AtomicLong clock = new AtomicLong(1_000_000L);
        var store = new AccountActionTokenStore(new SecureRandom(), clock::get);
        var first = store.issue(12L, AccountActionTokenStore.Purpose.VERIFY_EMAIL);
        assertThat(first.token()).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(first.toString()).doesNotContain(first.token());
        assertThat(store.consume(13L, AccountActionTokenStore.Purpose.VERIFY_EMAIL, first.token()))
            .isFalse();
        assertThat(store.consume(12L, AccountActionTokenStore.Purpose.RESET_PASSWORD, first.token()))
            .isFalse();
        assertThat(store.consume(12L, AccountActionTokenStore.Purpose.VERIFY_EMAIL, first.token()))
            .isTrue();
        assertThat(store.consume(12L, AccountActionTokenStore.Purpose.VERIFY_EMAIL, first.token()))
            .isFalse();
    }

    @Test
    void expiredAndReplacedChallengesCannotBeRedeemed() {
        AtomicLong clock = new AtomicLong(1_000_000L);
        var store = new AccountActionTokenStore(new SecureRandom(), clock::get);
        var older = store.issue(22L, AccountActionTokenStore.Purpose.RESET_PASSWORD);
        var newer = store.issue(22L, AccountActionTokenStore.Purpose.RESET_PASSWORD);
        assertThat(newer.token()).isNotEqualTo(older.token());
        assertThat(store.consume(22L, AccountActionTokenStore.Purpose.RESET_PASSWORD, older.token()))
            .isFalse();
        clock.addAndGet(Duration.ofMinutes(15).toMillis());
        assertThat(store.consume(22L, AccountActionTokenStore.Purpose.RESET_PASSWORD, newer.token()))
            .isFalse();
        assertThat(store.consume(22L, AccountActionTokenStore.Purpose.RESET_PASSWORD, newer.token()))
            .isFalse();
    }

    @Test
    void invalidInputFailsClosedAndCannotCrossPurposes() {
        var store = new AccountActionTokenStore();
        assertThatThrownBy(() -> store.issue(0, AccountActionTokenStore.Purpose.RESET_PASSWORD))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.issue(1, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(store.consume(1L, AccountActionTokenStore.Purpose.RESET_PASSWORD, null))
            .isFalse();
        assertThat(store.consume(1L, AccountActionTokenStore.Purpose.RESET_PASSWORD, "=HYPERLINK"))
            .isFalse();
        assertThat(store.consume(0L, AccountActionTokenStore.Purpose.RESET_PASSWORD, "x".repeat(43)))
            .isFalse();
    }

    @Test
    void racingRequestsCanConsumeExactlyOnce() throws Exception {
        var store = new AccountActionTokenStore();
        var token = store.issue(15L, AccountActionTokenStore.Purpose.VERIFY_EMAIL);
        var executor = Executors.newFixedThreadPool(8);
        var accepted = new AtomicInteger();
        try {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 40; i++) futures.add(executor.submit(() -> {
                if (store.consume(15L, AccountActionTokenStore.Purpose.VERIFY_EMAIL,
                        token.token())) accepted.incrementAndGet();
            }));
            for (var future : futures) future.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertThat(accepted.get()).isEqualTo(1);
    }
}
