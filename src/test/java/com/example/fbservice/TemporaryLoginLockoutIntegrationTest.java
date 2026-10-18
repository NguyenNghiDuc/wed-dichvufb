package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w1temporaryaccountlock;DB_CLOSE_DELAY=-1")
class TemporaryLoginLockoutIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    private void create(String name) {
        User u=new User();
        u.setUsername(name);
        u.setRole("USER");
        u.setPassword(encoder.encode("strong-test-credential-2026"));
        users.saveAndFlush(u);
    }
    @Test
    void wrongPasswordAttemptsAcrossDifferentPeersTemporarilyLockOneAccountOnly()
            throws Exception {
        create("lock-target-user");
        create("lock-unrelated-user");
        for(int i=0;i<20;i++) {
            final int peer=i/5;
            mvc.perform(post("/login").with(csrf()).with(req->{
                    req.setRemoteAddr("198.51.100."+(110+peer));
                    return req;
                }).param("username","lock-target-user")
                  .param("password","incorrect-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
        }
        // Even the correct password is not accepted while the one-account lock is active.
        mvc.perform(post("/login").with(csrf()).with(req->{
                req.setRemoteAddr("198.51.100.120");return req;
            }).param("username","lock-target-user")
              .param("password","strong-test-credential-2026"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"))
            .andExpect(header().string("Cache-Control",containsString("no-store")));

        // The account-wide lock does not lock an unrelated user behind the same IP.
        mvc.perform(post("/login").with(csrf()).with(req->{
                req.setRemoteAddr("198.51.100.120");return req;
            }).param("username","lock-unrelated-user")
              .param("password","strong-test-credential-2026"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns"));
    }

    @Test
    void lockedAndUnknownAccountsUseIdenticalPublicFailureRedirects() throws Exception {
        create("lock-private-name");
        for(int i=0;i<20;i++) {
            final int peer=i/5;
            mvc.perform(post("/login").with(csrf()).with(req->{
                req.setRemoteAddr("198.51.100."+(130+peer));return req;
            }).param("username","lock-private-name")
              .param("password","incorrect-password"))
                .andExpect(status().is3xxRedirection());
        }
        mvc.perform(post("/login").with(csrf()).with(req->{
            req.setRemoteAddr("198.51.100.140");return req;
        }).param("username","lock-private-name").param("password","incorrect-password"))
            .andExpect(redirectedUrl("/login?error"));
        mvc.perform(post("/login").with(csrf()).with(req->{
            req.setRemoteAddr("198.51.100.140");return req;
        }).param("username","lock-nonexistent-name").param("password","incorrect-password"))
            .andExpect(redirectedUrl("/login?error"));
    }
}
