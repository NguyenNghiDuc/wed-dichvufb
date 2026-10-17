package com.example.fbservice;

import com.example.fbservice.config.DatabaseUserDetailsService;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:unknownroles;DB_CLOSE_DELAY=-1")
class InvalidRoleAuthenticationIntegrationTest {
    @Autowired UserRepository users;
    @Autowired MockMvc mvc;
    @Autowired DatabaseUserDetailsService details;
    @Autowired PasswordEncoder encoder;

    @Test
    void unknownDatabaseRoleIsRejectedBeforeAuthentication() throws Exception {
        User u=new User();
        u.setUsername("corrupt-role-account");
        u.setRole("SUPERUSER");
        u.setPassword(encoder.encode("some-valid-password"));
        users.saveAndFlush(u);

        assertThatThrownBy(() -> details.loadUserByUsername("corrupt-role-account"))
            .isInstanceOf(UsernameNotFoundException.class);
        mvc.perform(post("/login").with(csrf())
            .param("username","corrupt-role-account")
            .param("password","some-valid-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void disabledValidRoleCannotLogin() throws Exception {
        User u=new User();
        u.setUsername("disabled-valid-role");
        u.setRole("USER");
        u.setEnabled(false);
        u.setPassword(encoder.encode("some-valid-password"));
        users.saveAndFlush(u);
        mvc.perform(post("/login").with(csrf())
            .param("username","disabled-valid-role")
            .param("password","some-valid-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }
}
