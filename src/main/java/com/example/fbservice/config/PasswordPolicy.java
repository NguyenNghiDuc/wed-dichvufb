package com.example.fbservice.config;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Local, provider-independent password strength guard for NEW passwords only.
 * Existing passwords remain usable for login so upgrades do not strand accounts.
 * No password values are logged or sent to external services.
 */
public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static void requireStrong(String password, String username) {
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException(
                "Mật khẩu cần ít nhất 12 ký tự và tối đa 72 byte UTF-8.");
        if (password.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Mật khẩu không được chứa ký tự điều khiển.");

        String normalized = Normalizer.normalize(password, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        String account = username == null ? "" : username.toLowerCase(Locale.ROOT);
        if (account.length() >= 4 && normalized.contains(account))
            throw new IllegalArgumentException(
                "Mật khẩu không được chứa tên đăng nhập.");
        // Reject common dictionary patterns even if padded with digits.
        if (normalized.matches("password[0-9]*")
                || normalized.matches("qwerty[0-9]*")
                || normalized.matches("admin[0-9]*")
                || normalized.matches("123456789[0-9]*")
                || normalized.matches("letmein[0-9]*")
                || normalized.matches("iloveyou[0-9]*")
                || normalized.codePoints().distinct().count() == 1)
            throw new IllegalArgumentException(
                "Mật khẩu quá phổ biến hoặc dễ đoán. Hãy dùng một cụm từ dài, riêng biệt.");
    }
}
