package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.security.SecurityActivity;
import com.example.fbservice.security.SecurityActivityRepository;
import com.example.fbservice.security.SecurityActivityService;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:securityactivity;DB_CLOSE_DELAY=-1")
class AccountSecurityActivityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired SecurityActivityRepository events;
    @Autowired SecurityActivityService history;

    private User create(String username) {
        User account=new User();
        account.setUsername(username);
        account.setRole("USER");
        account.setPassword(encoder.encode("secure-activity-password"));
        return users.saveAndFlush(account);
    }

    private MockHttpSession login(String username) throws Exception {
        var result=mvc.perform(post("/login").with(csrf())
            .param("username",username).param("password","secure-activity-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns")).andReturn();
        MockHttpSession session=(MockHttpSession)result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    @Test
    void recordsLoginFailuresAndShowsPrivateAccountHistory() throws Exception {
        User alice=create("security-activity-alice");
        User bob=create("security-activity-bob");

        mvc.perform(post("/login").with(csrf()).param("username",alice.getUsername())
            .param("password","wrong-password")).andExpect(redirectedUrl("/login?error"));
        assertThat(events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(alice.getId()))
            .extracting(SecurityActivity::getKind)
            .containsExactly(SecurityActivity.Kind.SIGN_IN_FAILED);

        MockHttpSession aliceSession=login(alice.getUsername());
        MockHttpSession bobSession=login(bob.getUsername());
        assertThat(events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(alice.getId()))
            .extracting(SecurityActivity::getKind)
            .containsExactly(SecurityActivity.Kind.SIGN_IN, SecurityActivity.Kind.SIGN_IN_FAILED);
        assertThat(events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(bob.getId()))
            .extracting(SecurityActivity::getKind)
            .containsExactly(SecurityActivity.Kind.SIGN_IN);

        // Only Alice's event is changed. Bob must not see it.
        mvc.perform(post("/account/password").session(aliceSession).with(csrf())
            .param("currentPassword","secure-activity-password")
            .param("newPassword","new-activity-password-123")
            .param("confirmPassword","new-activity-password-123"))
            .andExpect(redirectedUrl("/login?passwordChanged"));
        assertThat(events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(alice.getId()))
            .extracting(SecurityActivity::getKind)
            .contains(SecurityActivity.Kind.PASSWORD_CHANGED,SecurityActivity.Kind.SIGN_IN);
        assertThat(events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(bob.getId()))
            .extracting(SecurityActivity::getKind)
            .containsExactly(SecurityActivity.Kind.SIGN_IN);
        mvc.perform(get("/account/security").session(bobSession))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("Đã đổi mật khẩu"))));
        mvc.perform(get("/account/security"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/account/security").session(bobSession).with(csrf()))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void recordsRevocationAndBoundsHistoryToTwentyEntries() throws Exception {
        User owner=create("security-history-owner");
        MockHttpSession session=login(owner.getUsername());
        mvc.perform(post("/account/sessions/revoke").session(session).with(csrf())
            .param("currentPassword","secure-activity-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?sessionsRevoked"));

        var current=events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(owner.getId());
        assertThat(current).extracting(SecurityActivity::getKind)
            .contains(SecurityActivity.Kind.SIGN_IN,SecurityActivity.Kind.SESSIONS_REVOKED);

        for(int i=0;i<25;i++){
            history.record(owner.getId(),SecurityActivity.Kind.SIGN_IN);
        }
        assertThat(events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(owner.getId())).hasSize(20);
    }
}
