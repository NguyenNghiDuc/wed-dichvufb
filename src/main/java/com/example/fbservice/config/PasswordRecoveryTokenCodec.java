package com.example.fbservice.config;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * INTERNAL ONLY. Preparation for a future verified-email password recovery flow.
 * This class deliberately registers NO REST endpoint and sends NO email.
 *
 * Tokens expire in 15 minutes, bind to account ID and encoded-password revision,
 * and become unusable immediately after a successful password rotation.
 * Callers must atomically recheck the password revision in a DB write transaction
 * before consuming a token; verification alone does NOT consume it.
 */
public final class PasswordRecoveryTokenCodec {
    private static final int NONCE_BYTES=16;
    private static final int PAYLOAD_BYTES=1+Long.BYTES+Long.BYTES+NONCE_BYTES;
    private static final int SIGNATURE_BYTES=32;
    private static final Duration LIFETIME=Duration.ofMinutes(15);
    private final byte[] secret;
    private final Clock clock;
    private final SecureRandom random;

    /** Never pass a literal or generated default secret in production. */
    public PasswordRecoveryTokenCodec(byte[] secret) {
        this(secret,Clock.systemUTC(),new SecureRandom());
    }

    PasswordRecoveryTokenCodec(byte[] secret,Clock clock,SecureRandom random) {
        if(secret==null || secret.length<32)
            throw new IllegalArgumentException("An external recovery HMAC key of at least 32 bytes is required");
        this.secret=secret.clone();
        this.clock=Objects.requireNonNull(clock);
        this.random=Objects.requireNonNull(random);
    }

    public String issue(long accountId, String currentEncodedPassword) {
        if(accountId<=0 || currentEncodedPassword==null || currentEncodedPassword.isBlank())
            throw new IllegalArgumentException("Account ID and stored password revision required");
        byte[] nonce=new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        byte[] payload=ByteBuffer.allocate(PAYLOAD_BYTES)
            .put((byte)1).putLong(accountId).putLong(clock.instant().getEpochSecond())
            .put(nonce).array();
        byte[] signature=sign(payload,currentEncodedPassword);
        byte[] complete=ByteBuffer.allocate(PAYLOAD_BYTES+SIGNATURE_BYTES)
            .put(payload).put(signature).array();
        Arrays.fill(nonce,(byte)0);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(complete);
    }

    public boolean isValidForAccount(String token,long expectedAccountId,
                                     String currentEncodedPassword) {
        if(token==null || token.length()>128 || expectedAccountId<=0
                || currentEncodedPassword==null || currentEncodedPassword.isBlank()) return false;
        final byte[] decoded;
        try {
            decoded=Base64.getUrlDecoder().decode(token);
        } catch (IllegalArgumentException malformed) {
            return false;
        }
        if(decoded.length!=PAYLOAD_BYTES+SIGNATURE_BYTES) return false;
        byte[] payload=Arrays.copyOf(decoded,PAYLOAD_BYTES);
        byte[] given=Arrays.copyOfRange(decoded,PAYLOAD_BYTES,decoded.length);
        ByteBuffer buffer=ByteBuffer.wrap(payload);
        if(buffer.get()!=(byte)1 || buffer.getLong()!=expectedAccountId) return false;
        long issued=buffer.getLong();
        long now=clock.instant().getEpochSecond();
        // Tolerate one minute of reasonable sender/receiver clock skew.
        if(issued>now+60 || now-issued>LIFETIME.getSeconds())return false;
        byte[] expected=sign(payload,currentEncodedPassword);
        return MessageDigest.isEqual(expected,given);
    }

    private byte[] sign(byte[] payload,String passwordRevision) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret,"HmacSHA256"));
            mac.update(payload);
            mac.update(passwordRevision.getBytes(StandardCharsets.UTF_8));
            return mac.doFinal();
        } catch (java.security.InvalidKeyException | NoSuchAlgorithmException failure) {
            throw new IllegalStateException("Required HMAC-SHA256 unavailable",failure);
        }
    }
}
