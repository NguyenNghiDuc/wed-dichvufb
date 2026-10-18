package com.example.fbservice.config;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PasswordRecoveryTokenCodecTest {
    private static final byte[] TEST_KEY=new byte[32];

    @Test
    void tokenIsUrlSafeRandomAccountBoundAndInvalidAfterPasswordRotation() {
        var clock=Clock.fixed(Instant.parse("2026-10-18T09:00:00Z"),ZoneOffset.UTC);
        var codec=new PasswordRecoveryTokenCodec(TEST_KEY,clock,new SecureRandom());
        String issued=codec.issue(102L,"$2a$10$old-test-revision-not-a-real-password");
        String other=codec.issue(102L,"$2a$10$old-test-revision-not-a-real-password");
        assertThat(issued).matches("[A-Za-z0-9_-]+").isNotEqualTo(other);
        assertThat(codec.isValidForAccount(issued,102L,
            "$2a$10$old-test-revision-not-a-real-password")).isTrue();
        assertThat(codec.isValidForAccount(issued,103L,
            "$2a$10$old-test-revision-not-a-real-password")).isFalse();
        // The exact same link becomes invalid once the account password changes.
        assertThat(codec.isValidForAccount(issued,102L,
            "$2a$10$changed-test-revision-not-a-real-password")).isFalse();
        char different=issued.charAt(0) == 'A' ? 'B' : 'A';
        String altered=different+issued.substring(1);
        assertThat(codec.isValidForAccount(altered,102L,
            "$2a$10$old-test-revision-not-a-real-password")).isFalse();
    }

    @Test
    void expiryFutureIssueTimeTamperingAndWrongSecretFailClosed() {
        var atIssue=Clock.fixed(Instant.parse("2026-10-18T09:00:00Z"),ZoneOffset.UTC);
        var issuer=new PasswordRecoveryTokenCodec(TEST_KEY,atIssue,new SecureRandom());
        String token=issuer.issue(9L,"bcrypt-revision-1");
        var after16Minutes=new PasswordRecoveryTokenCodec(TEST_KEY,
            Clock.fixed(Instant.parse("2026-10-18T09:16:00Z"),ZoneOffset.UTC),new SecureRandom());
        assertThat(after16Minutes.isValidForAccount(token,9L,"bcrypt-revision-1")).isFalse();
        var beforeIssue=new PasswordRecoveryTokenCodec(TEST_KEY,
            Clock.fixed(Instant.parse("2026-10-18T08:58:00Z"),ZoneOffset.UTC),new SecureRandom());
        assertThat(beforeIssue.isValidForAccount(token,9L,"bcrypt-revision-1")).isFalse();
        byte[] altered=Base64.getUrlDecoder().decode(token);
        altered[15]^=1;
        String forged=Base64.getUrlEncoder().withoutPadding().encodeToString(altered);
        assertThat(issuer.isValidForAccount(forged,9L,"bcrypt-revision-1")).isFalse();
        byte[] otherSecret=new byte[32];otherSecret[0]=42;
        var differentApp=new PasswordRecoveryTokenCodec(otherSecret,atIssue,new SecureRandom());
        assertThat(differentApp.isValidForAccount(token,9L,"bcrypt-revision-1")).isFalse();
        assertThat(issuer.isValidForAccount("invalid###",9L,"bcrypt-revision-1")).isFalse();
    }

    @Test
    void rejectsMissingOrWeakKeyAndInvalidAccount() {
        assertThatThrownBy(() -> new PasswordRecoveryTokenCodec(new byte[16]))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PasswordRecoveryTokenCodec(null))
            .isInstanceOf(IllegalArgumentException.class);
        var issuer=new PasswordRecoveryTokenCodec(TEST_KEY);
        assertThatThrownBy(() -> issuer.issue(0,"bcrypt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> issuer.issue(1,""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(issuer.isValidForAccount("",1,"bcrypt")).isFalse();
    }
}
