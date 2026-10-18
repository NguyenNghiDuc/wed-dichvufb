package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.security.*;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:securityactivityexport;DB_CLOSE_DELAY=-1")
class SecurityActivityExportIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired SecurityActivityService history;

    private User create(String name) {
        User u=new User();u.setUsername(name);u.setRole("USER");
        u.setPassword(encoder.encode("a-secure-test-password"));
        return users.saveAndFlush(u);
    }

    private MockHttpSession login(String name) throws Exception {
        var login=mvc.perform(post("/login").with(csrf())
                .param("username",name)
                .param("password","a-secure-test-password"))
            .andExpect(redirectedUrl("/campaigns")).andReturn();
        return (MockHttpSession)login.getRequest().getSession(false);
    }

    @Test
    void csvExportIsBoundedPrivateAndContainsNoUserSecrets() throws Exception {
        User alice=create("export-alice");
        User bob=create("export-bob");
        var aliceSession=login(alice.getUsername());
        var bobSession=login(bob.getUsername());

        history.record(alice.getId(),SecurityActivity.Kind.PASSWORD_CHANGED);
        for (int i=0;i<205;i++) {
            history.record(bob.getId(),SecurityActivity.Kind.SESSIONS_REVOKED);
        }

        var aliceCsv=mvc.perform(get("/account/security/export").session(aliceSession))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"growlab-security-activity.csv\""))
            .andReturn().getResponse().getContentAsString();

        assertThat(aliceCsv).startsWith("time_utc,event_kind");
        assertThat(aliceCsv).contains("PASSWORD_CHANGED","SIGN_IN");
        assertThat(aliceCsv).doesNotContain("SESSIONS_REVOKED","a-secure-test-password","export-alice");

        var bobCsv=mvc.perform(get("/account/security/export").session(bobSession))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(bobCsv.split("\\r?\\n")).hasSize(201); // 1 header + 200 bounded events
        assertThat(bobCsv).doesNotContain("PASSWORD_CHANGED");

        mvc.perform(get("/account/security/export")).andExpect(status().is3xxRedirection());
    }
}
