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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:sessionlifecycle;DB_CLOSE_DELAY=-1")
class SessionLifecycleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void successfulLoginRotatesSessionIdAndLogoutRequiresCsrf() throws Exception {
        User u=new User();
        u.setUsername("session-lifecycle-owner");
        u.setRole("USER");
        u.setPassword(encoder.encode("secure-session-password"));
        users.saveAndFlush(u);

        var loginPage=mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        MockHttpSession preLogin = (MockHttpSession) loginPage.getRequest().getSession(false);
        assertThat(preLogin).isNotNull();
        String anonymousId=preLogin.getId();

        var login=mvc.perform(post("/login").session(preLogin).with(csrf())
                .param("username","session-lifecycle-owner")
                .param("password","secure-session-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns")).andReturn();
        MockHttpSession authenticated=(MockHttpSession)login.getRequest().getSession(false);
        assertThat(authenticated).isNotNull();
        assertThat(authenticated.getId()).isNotEqualTo(anonymousId);
        mvc.perform(get("/campaigns").session(authenticated)).andExpect(status().isOk());

        mvc.perform(post("/logout").session(authenticated))
            .andExpect(status().isForbidden());
        mvc.perform(get("/campaigns").session(authenticated)).andExpect(status().isOk());

        mvc.perform(post("/logout").session(authenticated).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
        mvc.perform(get("/campaigns")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void anonymousCannotCallSecurityAccountFeatures() throws Exception {
        mvc.perform(get("/account/password")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/account/sessions")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/account/sessions/revoke").with(csrf())
            .param("currentPassword","any-password")).andExpect(status().is3xxRedirection());
    }
}
