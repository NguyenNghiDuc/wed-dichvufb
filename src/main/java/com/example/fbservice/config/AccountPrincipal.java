package com.example.fbservice.config;

import com.example.fbservice.model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * Database-backed authenticated principal. A stable SHA-256 digest of the *encoded*
 * password allows revocation after a password change even if Spring Security erases
 * the UserDetails credentials after authentication.
 */
public final class AccountPrincipal extends org.springframework.security.core.userdetails.User {
    private static final long serialVersionUID = 1L;
    private final Long accountId;
    private final byte[] credentialRevision;

    public AccountPrincipal(User account, String role) {
        super(account.getUsername(), account.getPassword(), account.isEnabled(), true,
              true, true, AuthorityUtils.createAuthorityList("ROLE_" + role));
        this.accountId = account.getId();
        this.credentialRevision = digest(account.getPassword());
    }

    public Long getAccountId() {
        return accountId;
    }

    /** Digest compared in constant time; neither value is sent to clients or logs. */
    public boolean isCredentialCurrent(String encodedPassword) {
        return encodedPassword != null
            && MessageDigest.isEqual(credentialRevision, digest(encodedPassword));
    }

    private static byte[] digest(String encodedPassword) {
        try {
            return MessageDigest.getInstance("SHA-256")
                .digest(encodedPassword.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Required SHA-256 algorithm unavailable", impossible);
        }
    }
}
