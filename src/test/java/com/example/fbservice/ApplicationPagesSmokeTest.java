package com.example.fbservice;

import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:growlabsmoketest;DB_CLOSE_DELAY=-1")
class ApplicationPagesSmokeTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;

    @Test
    void publicPagesRender() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("GrowLab")));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/style.css")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "reader")
    void authenticatedViewsRender() throws Exception {
        mvc.perform(get("/campaigns")).andExpect(status().isOk());
        mvc.perform(get("/campaigns/new")).andExpect(status().isOk());
        mvc.perform(get("/order/new")).andExpect(redirectedUrl("/campaigns/new"));
        mvc.perform(get("/deposit/new")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "moderator", roles = "ADMIN")
    void adminViewRenders() throws Exception {
        mvc.perform(get("/admin/campaign-orders")).andExpect(status().isOk());
    }

    @Test
    void registrationHashesPasswordAndLogsIn() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("username", "smoketest_user_001")
                .param("password", "strongpassword123")
                .param("email", "smoketest@example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));
        var saved = users.findByUsername("smoketest_user_001").orElseThrow();
        assertThat(saved.getPassword()).startsWith("$2");
        assertThat(saved.getRole()).isEqualTo("USER");
        mvc.perform(post("/login").with(csrf())
                .param("username", "smoketest_user_001")
                .param("password", "strongpassword123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }
}
