package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:publicloginrevocation;DB_CLOSE_DELAY=-1")
class PublicLoginSessionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void disabledAccountCanAccessLoginButNotPrivatePagesWithOldCookie() throws Exception {
        User account=new User();
        account.setUsername("session-recovery-user");
        account.setRole("USER");
        account.setPassword(encoder.encode("securePassphrase456"));
        account=users.save(account);

        var result=mvc.perform(post("/login").with(csrf())
                .param("username",account.getUsername())
                .param("password","securePassphrase456"))
            .andExpect(status().is3xxRedirection()).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        account.setEnabled(false);
        users.saveAndFlush(account);

        // Crucial production symptom: /login must be a valid public GET with a stale cookie.
        mvc.perform(get("/login").session(session)
                .header("X-Forwarded-Proto","https")
                .header("X-Forwarded-Host","example-8000.app.github.dev"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-GrowLab-Auth-Revision","session-recovery-v2"));

        mvc.perform(get("/campaigns").session(session))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login?sessionExpired"));

        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/admin/campaign-orders")).andExpect(status().is3xxRedirection());
    }

    @Test
    void anonymousPostWithoutCsrfCannotModifyAccount() throws Exception {
        mvc.perform(post("/register")
            .param("username","blocked-no-csrf")
            .param("password","securePassphrase456"))
            .andExpect(status().isForbidden());
        assertThat(users.existsByUsername("blocked-no-csrf")).isFalse();
    }
}
