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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:credentialrevision;DB_CLOSE_DELAY=-1")
class CredentialRevisionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void rotatingEncodedPasswordRevokesOldSessionButKeepsPublicLoginAvailable() throws Exception {
        User user = new User();
        user.setUsername("revision-owner");
        user.setRole("USER");
        user.setPassword(encoder.encode("previous-secure-secret"));
        user = users.saveAndFlush(user);

        var login = mvc.perform(post("/login").with(csrf())
                .param("username", "revision-owner")
                .param("password", "previous-secure-secret"))
            .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mvc.perform(get("/campaigns").session(session)).andExpect(status().isOk());

        user.setPassword(encoder.encode("replacement-secure-secret"));
        users.saveAndFlush(user);

        mvc.perform(get("/campaigns").session(session))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login?sessionExpired"));
        mvc.perform(get("/login")).andExpect(status().isOk());

        mvc.perform(post("/login").with(csrf())
                .param("username", "revision-owner")
                .param("password", "previous-secure-secret"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
        mvc.perform(post("/login").with(csrf())
                .param("username", "revision-owner")
                .param("password", "replacement-secure-secret"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns"));
    }
}
