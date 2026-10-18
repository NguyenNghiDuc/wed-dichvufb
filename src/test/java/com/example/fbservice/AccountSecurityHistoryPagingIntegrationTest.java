package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.security.SecurityActivity;
import com.example.fbservice.security.SecurityActivityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w1securityhistorypage;DB_CLOSE_DELAY=-1")
class AccountSecurityHistoryPagingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired SecurityActivityRepository events;
    @Autowired PasswordEncoder encoder;

    private User create(String name) {
        User user = new User();
        user.setUsername(name);
        user.setPassword(encoder.encode("only-test-password-for-history"));
        user.setRole("USER");
        return users.saveAndFlush(user);
    }

    private MockHttpSession login(String username) throws Exception {
        var result = mvc.perform(post("/login").with(csrf())
                .param("username",username).param("password","only-test-password-for-history"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns")).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    @Test
    void userCanPageOnlyTheirOwnSecurityEvents() throws Exception {
        User alice=create("w1-history-pages-alice");
        User bob=create("w1-history-pages-bob");
        MockHttpSession session=login(alice.getUsername());
        for(int i=0;i<25;i++)
            events.save(new SecurityActivity(alice.getId(),SecurityActivity.Kind.SIGN_IN_FAILED));
        for(int i=0;i<12;i++)
            events.save(new SecurityActivity(bob.getId(),SecurityActivity.Kind.PASSWORD_CHANGED));
        events.flush();

        var first=mvc.perform(get("/account/security").session(session))
            .andExpect(status().isOk())
            .andExpect(model().attribute("events",hasSize(20)))
            .andExpect(model().attribute("eventCount",26L))
            .andExpect(content().string(containsString("Trang 1 / 2")))
            .andReturn().getResponse().getContentAsString();
        var second=mvc.perform(get("/account/security").session(session).param("page","1"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("events",hasSize(6)))
            .andExpect(content().string(containsString("Trang 2 / 2")))
            .andReturn().getResponse().getContentAsString();
        assertThat(first).doesNotContain("Đã đổi mật khẩu");
        assertThat(second).doesNotContain("Đã đổi mật khẩu");
        assertThat(events.findTop200ByAccountIdOrderByCreatedAtDescIdDesc(alice.getId()))
            .hasSize(26);
    }

    @Test
    void invalidPagesAndAnonymousRequestsAreRejected() throws Exception {
        User alice=create("w1-history-page-invalid");
        MockHttpSession session=login(alice.getUsername());
        mvc.perform(get("/account/security").session(session).param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/account/security").session(session).param("page","10001"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/account/security").session(session).param("page","not-a-number"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/account/security"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/account/security").session(session).with(csrf()))
            .andExpect(status().isMethodNotAllowed());
    }
}
