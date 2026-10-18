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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w1sessiondetails;DB_CLOSE_DELAY=-1")
class AccountCurrentSessionDetailsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void currentBrowserSessionShowsBoundedMetadataEscapedAndNotForwardedHost() throws Exception {
        User owner=new User();
        owner.setUsername("current-browser-owner");
        owner.setRole("USER");
        owner.setPassword(encoder.encode("browser-session-secret"));
        users.saveAndFlush(owner);
        var login=mvc.perform(post("/login").with(csrf())
                .param("username","current-browser-owner")
                .param("password","browser-session-secret"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns")).andReturn();
        MockHttpSession session=(MockHttpSession)login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mvc.perform(get("/account/sessions").session(session)
                    .with(req -> { req.setRemoteAddr("198.51.100.240"); return req; })
                    .header("User-Agent","<script>alert(1)</script>")
                    .header("X-Forwarded-For","203.0.113.240"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("sessionCreatedUtc","sessionLastAccessUtc"))
            .andExpect(model().attribute("sessionTimeoutMinutes",30))
            .andExpect(content().string(containsString("198.51.100.240")))
            .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
            .andExpect(content().string(not(containsString("203.0.113.240"))));
        mvc.perform(get("/account/sessions")).andExpect(status().is3xxRedirection());
    }
}
