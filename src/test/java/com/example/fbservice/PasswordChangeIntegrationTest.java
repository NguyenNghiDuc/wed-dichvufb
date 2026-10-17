package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:passwordchange;DB_CLOSE_DELAY=-1")
class PasswordChangeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    private User create(String username, String original) {
        User u = new User();
        u.setUsername(username);
        u.setRole("USER");
        u.setPassword(encoder.encode(original));
        return users.saveAndFlush(u);
    }

    private MockHttpSession login(String username, String password) throws Exception {
        var response = mvc.perform(post("/login").with(csrf())
            .param("username", username).param("password", password))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns")).andReturn();
        MockHttpSession session=(MockHttpSession) response.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    @Test
    void passwordRotationInvalidatesBothCurrentAndOtherSessions() throws Exception {
        create("password-owner","old-secret-password");
        MockHttpSession session = login("password-owner","old-secret-password");
        MockHttpSession other = login("password-owner","old-secret-password");

        mvc.perform(get("/account/password").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Đổi mật khẩu")));

        mvc.perform(post("/account/password").session(session).with(csrf())
                .param("currentPassword","old-secret-password")
                .param("newPassword","replacement-secret-password")
                .param("confirmPassword","replacement-secret-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?passwordChanged"));

        assertThat(encoder.matches("replacement-secret-password",
            users.findByUsername("password-owner").orElseThrow().getPassword())).isTrue();
        mvc.perform(get("/campaigns").session(other))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login?sessionExpired"));

        mvc.perform(post("/login").with(csrf())
            .param("username","password-owner")
            .param("password","old-secret-password"))
            .andExpect(redirectedUrl("/login?error"));
        login("password-owner","replacement-secret-password");
    }

    @Test
    void rejectsWrongCurrentMismatchShortAndUnchangedPasswordsWithoutChangingCredential() throws Exception {
        User account=create("password-invalid","initial-passphrase");
        MockHttpSession session=login("password-invalid","initial-passphrase");
        String originalHash=account.getPassword();
        var request=post("/account/password").session(session).with(csrf());

        mvc.perform(post("/account/password").session(session).with(csrf())
                .param("currentPassword","wrong-secret")
                .param("newPassword","something-new-strong")
                .param("confirmPassword","something-new-strong"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("không chính xác")));

        mvc.perform(post("/account/password").session(session).with(csrf())
                .param("currentPassword","initial-passphrase")
                .param("newPassword","too-short")
                .param("confirmPassword","too-short"))
            .andExpect(status().isOk());

        mvc.perform(post("/account/password").session(session).with(csrf())
                .param("currentPassword","initial-passphrase")
                .param("newPassword","new-strong-passphrase")
                .param("confirmPassword","different-confirmation"))
            .andExpect(status().isOk());

        mvc.perform(post("/account/password").session(session).with(csrf())
                .param("currentPassword","initial-passphrase")
                .param("newPassword","initial-passphrase")
                .param("confirmPassword","initial-passphrase"))
            .andExpect(status().isOk());

        assertThat(users.findByUsername("password-invalid").orElseThrow().getPassword())
            .isEqualTo(originalHash);
    }

    @Test
    void csrfAndAuthenticationAreRequired() throws Exception {
        create("password-csrf","old-password-value");
        MockHttpSession session=login("password-csrf","old-password-value");
        mvc.perform(post("/account/password").session(session)
            .param("currentPassword","old-password-value")
            .param("newPassword","new-password-value")
            .param("confirmPassword","new-password-value"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/account/password"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/account/password").with(csrf())
            .param("currentPassword","old-password-value")
            .param("newPassword","new-password-value")
            .param("confirmPassword","new-password-value"))
            .andExpect(status().is3xxRedirection());
        assertThat(encoder.matches("old-password-value",
            users.findByUsername("password-csrf").orElseThrow().getPassword())).isTrue();
    }
}
