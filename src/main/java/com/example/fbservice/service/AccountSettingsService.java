package com.example.fbservice.service;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.notification.*;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountSettingsService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AccountNotificationRepository notifications;

    public AccountSettingsService(UserRepository users, PasswordEncoder encoder,
                                  AccountNotificationRepository notifications) {
        this.users = users;
        this.encoder = encoder;
        this.notifications = notifications;
    }

    @Transactional
    public void updateDisplayName(String username, String displayName) {
        String cleaned = displayName == null ? "" : displayName.strip();
        if (cleaned.length() > 80 || cleaned.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Tên hiển thị tối đa 80 ký tự và không chứa ký tự điều khiển.");
        }
        User account = account(username);
        account.setDisplayName(cleaned.isBlank() ? null : cleaned);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword, String confirmation) {
        User account = account(username);
        verifyPassword(account, currentPassword);
        if (newPassword == null || newPassword.length() < 8 ||
                newPassword.getBytes(StandardCharsets.UTF_8).length > 72 || newPassword.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu mới cần ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
        }
        if (!newPassword.equals(confirmation)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận chưa khớp.");
        }
        if (encoder.matches(newPassword, account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        account.setPassword(encoder.encode(newPassword));
        account.setAuthVersion(Math.addExact(account.getAuthVersion(), 1));
        notifications.save(new AccountNotification(username, "Đã đổi mật khẩu",
            "Mật khẩu đã được thay đổi. Các phiên đăng nhập trước đó đã bị thu hồi."));
    }

    @Transactional
    public void revokeSessions(String username, String currentPassword) {
        User account = account(username);
        verifyPassword(account, currentPassword);
        account.setAuthVersion(Math.addExact(account.getAuthVersion(), 1));
        notifications.save(new AccountNotification(username, "Đã đăng xuất tất cả phiên",
            "Các phiên đăng nhập trước đó đã bị thu hồi. Đăng nhập lại để tiếp tục sử dụng."));
    }

    private User account(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return users.lockByUsername(username).filter(User::isEnabled)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void verifyPassword(User account, String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72 ||
                !encoder.matches(password, account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại chưa đúng.");
        }
    }
}
