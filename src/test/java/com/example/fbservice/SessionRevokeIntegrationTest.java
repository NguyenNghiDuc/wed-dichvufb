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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:sessionrevoketest;DB_CLOSE_DELAY=-1")
class SessionRevokeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    private MockHttpSession login(String username,String password) throws Exception {
        var response=mvc.perform(post("/login").with(csrf())
            .param("username",username).param("password",password))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns")).andReturn();
        MockHttpSession session=(MockHttpSession)response.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    @Test
    void revokeAllKeepsPasswordButLogsOutOtherBrowsers() throws Exception {
        User u=new User();
        u.setUsername("revoke-owner");
        u.setRole("USER");
        u.setPassword(encoder.encode("very-safe-password"));
        u=users.saveAndFlush(u);
        String initialHash=u.getPassword();
        MockHttpSession first=login("revoke-owner","very-safe-password");
        MockHttpSession second=login("revoke-owner","very-safe-password");

        mvc.perform(get("/account/sessions").session(first))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng xuất tất cả thiết bị")));
        mvc.perform(post("/account/sessions/revoke").session(first).with(csrf())
            .param("currentPassword","very-safe-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?sessionsRevoked"));

        String revised=users.findByUsername("revoke-owner").orElseThrow().getPassword();
        assertThat(revised).isNotEqualTo(initialHash);
        assertThat(encoder.matches("very-safe-password",revised)).isTrue();
        mvc.perform(get("/campaigns").session(second))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login?sessionExpired"));
        login("revoke-owner","very-safe-password");
    }

    @Test
    void rejectsWrongPasswordAndMissingCsrf() throws Exception {
        User u=new User();u.setUsername("revoke-reject");
        u.setRole("USER");u.setPassword(encoder.encode("another-safe-password"));
        u=users.saveAndFlush(u);
        String original=u.getPassword();
        MockHttpSession session=login("revoke-reject","another-safe-password");

        mvc.perform(post("/account/sessions/revoke").session(session)
            .param("currentPassword","another-safe-password"))
            .andExpect(status().isForbidden());

        mvc.perform(post("/account/sessions/revoke").session(session).with(csrf())
            .param("currentPassword","wrong-password"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("không chính xác")));
        assertThat(users.findByUsername("revoke-reject").orElseThrow().getPassword())
            .isEqualTo(original);

        mvc.perform(get("/account/sessions")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/account/sessions/revoke").with(csrf())
            .param("currentPassword","another-safe-password"))
            .andExpect(status().is3xxRedirection());
    }
}
