package com.example.fbservice;

import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w1passwordpolicy;DB_CLOSE_DELAY=-1")
class PasswordPolicyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository accounts;
    @Autowired PasswordEncoder encoder;

    @Test
    void registrationRejectsWeakAndAccountNamePasswordsButAcceptsStrongPassphrase() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("username","weak-register-user")
                .param("password","password123456789"))
            .andExpect(status().isOk()).andExpect(view().name("register"))
            .andExpect(content().string(containsString("dễ đoán")));
        assertThat(accounts.existsByUsername("weak-register-user")).isFalse();

        mvc.perform(post("/register").with(csrf())
                .param("username","password-user")
                .param("password","password-user-2026!"))
            .andExpect(status().isOk()).andExpect(view().name("register"))
            .andExpect(content().string(containsString("tên đăng nhập")));
        assertThat(accounts.existsByUsername("password-user")).isFalse();

        mvc.perform(post("/register").with(csrf())
                .param("username","policy-strong-register")
                .param("password","A calm orange river 2026!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));
        assertThat(encoder.matches("A calm orange river 2026!",
            accounts.findByUsername("policy-strong-register").orElseThrow().getPassword())).isTrue();
    }

    @Test
    void passwordChangeDeniesWeakWithoutMutatingCurrentSecret() throws Exception {
        User user=new User();
        user.setUsername("policy-change-owner");
        user.setRole("USER");
        user.setPassword(encoder.encode("original-private-passphrase"));
        user=accounts.saveAndFlush(user);
        String hash=user.getPassword();
        var signed=mvc.perform(post("/login").with(csrf())
                    .param("username","policy-change-owner")
                    .param("password","original-private-passphrase"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/campaigns")).andReturn();
        var session=(MockHttpSession)signed.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(post("/account/password").session(session).with(csrf())
                .param("currentPassword","original-private-passphrase")
                .param("newPassword","password123456789")
                .param("confirmPassword","password123456789"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("dễ đoán")));
        assertThat(accounts.findById(user.getId()).orElseThrow().getPassword())
            .isEqualTo(hash);
        mvc.perform(get("/campaigns").session(session)).andExpect(status().isOk());
    }
}
