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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:bcryptloginboundary;DB_CLOSE_DELAY=-1")
class BcryptLoginBoundaryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void loginRejectsSuffixAfter72BytesWithoutLockingOutLegitimateLogin() throws Exception {
        String exactly72="S".repeat(72);
        User u=new User();u.setUsername("boundary-owner");u.setRole("USER");
        u.setPassword(passwords.encode(exactly72));users.saveAndFlush(u);

        mvc.perform(post("/login").with(csrf())
            .param("username","boundary-owner")
            .param("password",exactly72+"unwanted-extra-suffix"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login?error"));

        var loggedIn=mvc.perform(post("/login").with(csrf())
            .param("username","boundary-owner")
            .param("password",exactly72))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns")).andReturn();
        MockHttpSession session=(MockHttpSession) loggedIn.getRequest().getSession(false);
        assertThat(session).isNotNull();

        String previousHash=users.findByUsername("boundary-owner").orElseThrow().getPassword();
        mvc.perform(post("/account/password").session(session).with(csrf())
            .param("currentPassword",exactly72+"another-suffix")
            .param("newPassword","a-new-secure-passphrase")
            .param("confirmPassword","a-new-secure-passphrase"))
            .andExpect(status().isOk());
        mvc.perform(post("/account/sessions/revoke").session(session).with(csrf())
            .param("currentPassword",exactly72+"another-suffix"))
            .andExpect(status().isOk());

        assertThat(users.findByUsername("boundary-owner").orElseThrow().getPassword())
            .isEqualTo(previousHash);
        mvc.perform(get("/campaigns").session(session)).andExpect(status().isOk());
    }
}
