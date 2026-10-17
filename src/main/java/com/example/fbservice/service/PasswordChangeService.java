package com.example.fbservice.service;

import com.example.fbservice.config.AccountPrincipal;
import com.example.fbservice.config.TrafficLimiter;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Self-service credential rotation with row locking to serialize concurrent updates.
 * No email provider or external identity integration is needed for this flow.
 */
@Service
public class PasswordChangeService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TrafficLimiter limiter;

    public PasswordChangeService(UserRepository users, PasswordEncoder passwords, TrafficLimiter limiter) {
        this.users = users;
        this.passwords = passwords;
        this.limiter = limiter;
    }

    /**
     * Signs out all sessions without changing the user's actual password.
     * Re-encoding the same password gives a fresh BCrypt salt and thus a fresh
     * credential revision. Subsequent protected requests reject old principals.
     */
    @Transactional
    public void revokeAll(AccountPrincipal principal, String currentPassword) {
        if (principal == null || principal.getAccountId() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (currentPassword == null || currentPassword.isBlank()
                || currentPassword.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");

        limiter.check("session-revoke:" + principal.getAccountId(), 10, Duration.ofHours(1));
        User account = users.lockById(principal.getAccountId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!account.isEnabled()
                || !account.getUsername().equals(principal.getUsername())
                || !principal.isCredentialCurrent(account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!passwords.matches(currentPassword, account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        account.setPassword(passwords.encode(currentPassword));
    }

    @Transactional
    public void change(AccountPrincipal principal, String currentPassword,
                       String newPassword, String confirmation) {
        if (principal == null || principal.getAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (currentPassword == null || currentPassword.isBlank()
                || currentPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");
        }
        if (newPassword == null || newPassword.length() < 12
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72
                || newPassword.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu mới cần từ 12 ký tự và tối đa 72 byte UTF-8.");
        }
        if (!newPassword.equals(confirmation)) {
            throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp.");
        }

        // Rate-limit per account to make repeated guessing of the existing password expensive.
        limiter.check("password-change:" + principal.getAccountId(), 10, Duration.ofHours(1));
        User account = users.lockById(principal.getAccountId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!account.isEnabled()
                || !account.getUsername().equals(principal.getUsername())
                || !principal.isCredentialCurrent(account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!passwords.matches(currentPassword, account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        if (passwords.matches(newPassword, account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        account.setPassword(passwords.encode(newPassword));
    }
}
