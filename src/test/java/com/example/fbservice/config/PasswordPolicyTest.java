package com.example.fbservice.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PasswordPolicyTest {
    @Test
    void deniesCommonRepetitiveUsernameBasedAndControlCharacterPasswords() {
        for(String weak: new String[]{"password123456789","QWERTY123456789",
                "123456789012","aaaaaaaaaaaa","admin123456789",
                "growlabUser!123456", "valid" + (char)0 + "control2026"}) {
            assertThatThrownBy(() -> PasswordPolicy.requireStrong(weak,"growlabUser"))
                .as("must reject "+weak.replace(String.valueOf((char)0),"?"))
                .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> PasswordPolicy.requireStrong("short123","owner"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PasswordPolicy.requireStrong("🔒".repeat(25),"owner"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsMemorablePassphrasesAndExistingApplicationTestExamples() {
        assertThatCode(() -> PasswordPolicy.requireStrong("A calm orange river 2026!","someone"))
            .doesNotThrowAnyException();
        assertThatCode(() -> PasswordPolicy.requireStrong("validSecret123456","random-user"))
            .doesNotThrowAnyException();
        assertThatCode(() -> PasswordPolicy.requireStrong(
            "replacement-secret-password","password-owner")).doesNotThrowAnyException();
        assertThatCode(() -> PasswordPolicy.requireStrong(
            "strong-throwaway-password","registered-after-csrf")).doesNotThrowAnyException();
    }
}
