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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w1stalerelogin;DB_CLOSE_DELAY=-1")
class StaleSessionReauthenticationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    private User user(String name, String role) {
        var u=new User();u.setUsername(name);u.setRole(role);
        u.setPassword(encoder.encode("strong-stale-login-password"));
        return users.saveAndFlush(u);
    }

    private MockHttpSession login(String username) throws Exception {
        var r=mvc.perform(post("/login").with(csrf())
              .param("username",username).param("password","strong-stale-login-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns")).andReturn();
        var session=(MockHttpSession)r.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    @Test
    void revokedSessionCanLoginAsAnotherAccountWithSameBrowserCookie() throws Exception {
        User former=user("w1-stale-locked","ADMIN");
        user("w1-stale-newuser","USER");
        MockHttpSession stale=login(former.getUsername());
        former.setEnabled(false);users.saveAndFlush(former);

        mvc.perform(get("/login").session(stale))
            .andExpect(status().isOk());
        // Still deny protected reads: don't make the stale session a privilege bypass.
        mvc.perform(get("/admin/dashboard").session(stale))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login?sessionExpired"));

        // A separate stale login session is needed because prior private GET
        // invalidated the first one. Reproduce the POST path directly instead.
        User old=user("w1-stale-post-user","ADMIN");
        MockHttpSession stalePost=login(old.getUsername());
        old.setEnabled(false);users.saveAndFlush(old);
        var second=mvc.perform(post("/login").session(stalePost).with(csrf())
                .param("username","w1-stale-newuser")
                .param("password","strong-stale-login-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns")).andReturn();
        var current=(MockHttpSession)second.getRequest().getSession(false);
        assertThat(current).isNotNull();
        mvc.perform(get("/campaigns").session(current)).andExpect(status().isOk());
        mvc.perform(get("/admin/dashboard").session(current)).andExpect(status().isForbidden());
    }

    @Test
    void staleCookieDoesNotBypassCsrfForPublicPosts() throws Exception {
        User former=user("w1-stale-csrf-old","USER");
        MockHttpSession session=login(former.getUsername());
        former.setEnabled(false);users.saveAndFlush(former);

        mvc.perform(post("/login").session(session)
                .param("username","w1-stale-csrf-old")
                .param("password","strong-stale-login-password"))
            .andExpect(status().isForbidden());

        mvc.perform(post("/register").session(session)
                .param("username","w1-stale-should-not-register")
                .param("password","A strong orange river 2026!"))
            .andExpect(status().isForbidden());
        assertThat(users.existsByUsername("w1-stale-should-not-register")).isFalse();

        mvc.perform(post("/register").session(session).with(csrf())
                .param("username","w1-stale-can-register")
                .param("password","A strong orange river 2026!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));
        assertThat(users.existsByUsername("w1-stale-can-register")).isTrue();
    }

    @Test
    void staleRoleChangeCannotWriteProtectedEndpoints() throws Exception {
        User former=user("w1-stale-role-change","ADMIN");
        MockHttpSession session=login(former.getUsername());
        former.setRole("USER");users.saveAndFlush(former);
        mvc.perform(post("/admin/catalog").session(session).with(csrf()))
            .andExpect(status().isUnauthorized());
    }
}
