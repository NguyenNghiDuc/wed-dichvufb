package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.security.SecurityActivity;
import com.example.fbservice.security.SecurityActivityRepository;
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
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:w1failedloginaudit;DB_CLOSE_DELAY=-1")
class FailedLoginAuditIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired SecurityActivityRepository events;

    private User account(String name, boolean enabled) {
        User account = new User();
        account.setUsername(name);
        account.setPassword(passwords.encode("correct-private-test-password"));
        account.setRole("USER");
        account.setEnabled(enabled);
        return users.saveAndFlush(account);
    }

    @Test
    void knownAccountFailureIsPrivateAndSuccessStillWorks() throws Exception {
        User alice = account("audit-failure-alice", true);
        User bob = account("audit-failure-bob", true);
        String client = "198.51.100.181";

        mvc.perform(post("/login").with(csrf()).with(r -> {
                    r.setRemoteAddr(client);
                    return r;
                }).param("username", alice.getUsername())
                .param("password", "incorrect-test-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"))
            .andExpect(header().string("Cache-Control", containsString("no-store")));

        assertThat(events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(alice.getId()))
            .extracting(SecurityActivity::getKind))
            .containsExactly(SecurityActivity.Kind.SIGN_IN_FAILED);
        assertThat(events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(bob.getId()))
            .isEmpty();

        var login=mvc.perform(post("/login").with(csrf()).with(r -> {
                    r.setRemoteAddr(client);
                    return r;
                }).param("username", alice.getUsername())
                .param("password", "correct-private-test-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns")).andReturn();
        var session=(MockHttpSession)login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/account/security").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Đăng nhập thất bại")));
        assertThat(events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(alice.getId()))
            .extracting(SecurityActivity::getKind))
            .containsExactly(SecurityActivity.Kind.SIGN_IN, SecurityActivity.Kind.SIGN_IN_FAILED);
    }

    @Test
    void unknownUserAndCsrfDenialNeverCreateEvents() throws Exception {
        User alice=account("audit-failure-guard", true);
        long before = events.count();
        mvc.perform(post("/login").with(csrf()).with(r -> {
                    r.setRemoteAddr("198.51.100.182");
                    return r;
                }).param("username", "nonexistent-account-182")
                .param("password", "anything"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
        mvc.perform(post("/login").with(r -> {
                    r.setRemoteAddr("198.51.100.183");
                    return r;
                }).param("username", alice.getUsername())
                .param("password", "wrong-password"))
            .andExpect(status().isForbidden());
        assertThat(events.count()).isEqualTo(before);
    }

    @Test
    void rateLimitedLoginDoesNotWriteMoreFailedEventsOrBlockAnotherAccount() throws Exception {
        User targeted=account("audit-failure-throttled", true);
        User unrelated=account("audit-failure-unrelated", true);
        // A fixed TCP peer is used instead of an untrusted X-Forwarded-For header.
        // Per-account attempts cap at ten per ten minutes.
        for (int i=0;i<10;i++) {
            final int index=i;
            mvc.perform(post("/login").with(csrf()).with(r -> {
                        r.setRemoteAddr("198.51.100.185");
                        r.addHeader("X-Forwarded-For","192.0.2."+(20+index));
                        return r;
                    }).param("username", targeted.getUsername())
                    .param("password","not-the-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
        }
        assertThat(events.findTop200ByAccountIdOrderByCreatedAtDescIdDesc(targeted.getId()))
            .extracting(SecurityActivity::getKind))
            .hasSize(10)
            .containsOnly(SecurityActivity.Kind.SIGN_IN_FAILED);

        mvc.perform(post("/login").with(csrf()).with(r -> {
                    r.setRemoteAddr("198.51.100.185");
                    r.addHeader("X-Forwarded-For","192.0.2.99");
                    return r;
                }).param("username",targeted.getUsername())
                .param("password","not-the-password"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After","600"));
        assertThat(events.findTop200ByAccountIdOrderByCreatedAtDescIdDesc(targeted.getId()))
            .hasSize(10);

        mvc.perform(post("/login").with(csrf()).with(r -> {
                    r.setRemoteAddr("198.51.100.185");
                    return r;
                }).param("username",unrelated.getUsername())
                .param("password","not-the-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
        assertThat(events.findTop200ByAccountIdOrderByCreatedAtDescIdDesc(unrelated.getId()))
            .extracting(SecurityActivity::getKind))
            .containsExactly(SecurityActivity.Kind.SIGN_IN_FAILED);
    }

    @Test
    void disabledAccountFailuresRemainGenericAndOwnerScoped() throws Exception {
        User blocked=account("audit-failure-locked", false);
        mvc.perform(post("/login").with(csrf()).with(r -> {
                    r.setRemoteAddr("198.51.100.184");
                    return r;
                }).param("username", blocked.getUsername())
                .param("password", "correct-private-test-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
        assertThat(events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(blocked.getId()))
            .extracting(SecurityActivity::getKind))
            .containsExactly(SecurityActivity.Kind.SIGN_IN_FAILED);
    }
}
