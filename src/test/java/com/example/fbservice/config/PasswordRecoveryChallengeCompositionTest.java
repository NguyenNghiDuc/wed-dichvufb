package com.example.fbservice.config;

import com.example.fbservice.security.AccountActionTokenStore;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Both existing internal primitives are complementary, not competing reset flows:
 * one signs/account+password-binds; the other rejects replay.
 */
class PasswordRecoveryChallengeCompositionTest {
    @Test
    void passwordRevisionBoundHmacTokenCanAlsoBeMadeOneTime() {
        var clock=Clock.fixed(Instant.parse("2026-10-18T09:00:00Z"),ZoneOffset.UTC);
        byte[] testKey=new byte[32];
        var codec=new PasswordRecoveryTokenCodec(testKey, clock, new SecureRandom());
        var replay=new AccountActionTokenStore();
        String revision="$2a$10$test-only-password-revision";
        String link=codec.issue(701L,revision);
        replay.rememberSigned(701L,AccountActionTokenStore.Purpose.RESET_PASSWORD,link);

        assertThat(codec.isValidForAccount(link,701L,revision)).isTrue();
        assertThat(replay.consume(701L,AccountActionTokenStore.Purpose.VERIFY_EMAIL,link))
            .isFalse();
        assertThat(replay.consume(702L,AccountActionTokenStore.Purpose.RESET_PASSWORD,link))
            .isFalse();
        assertThat(replay.consume(701L,AccountActionTokenStore.Purpose.RESET_PASSWORD,link))
            .isTrue();
        assertThat(replay.consume(701L,AccountActionTokenStore.Purpose.RESET_PASSWORD,link))
            .isFalse();
        assertThat(codec.isValidForAccount(link,701L,revision+"rotated")).isFalse();
    }

    @Test
    void replaceSignedTokenInvalidatesPreviouslyIssuedBearer() {
        byte[] secret=new byte[32];
        var codec=new PasswordRecoveryTokenCodec(secret);
        var replay=new AccountActionTokenStore();
        String old=codec.issue(811L,"bcrypt-revision");
        String newer=codec.issue(811L,"bcrypt-revision");
        replay.rememberSigned(811L,AccountActionTokenStore.Purpose.RESET_PASSWORD,old);
        replay.rememberSigned(811L,AccountActionTokenStore.Purpose.RESET_PASSWORD,newer);
        assertThat(replay.consume(811L,AccountActionTokenStore.Purpose.RESET_PASSWORD,old))
            .isFalse();
        assertThat(replay.consume(811L,AccountActionTokenStore.Purpose.RESET_PASSWORD,newer))
            .isTrue();
        assertThatThrownBy(() -> replay.rememberSigned(811L,
            AccountActionTokenStore.Purpose.RESET_PASSWORD,"https://untrusted.invalid"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
