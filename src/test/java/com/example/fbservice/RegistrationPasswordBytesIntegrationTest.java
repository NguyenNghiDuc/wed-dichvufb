package com.example.fbservice;

import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:registrationbytes;DB_CLOSE_DELAY=-1")
class RegistrationPasswordBytesIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    void rejectsMultiByteSecretBeyondBcryptEffectiveByteLimit() throws Exception {
        String tooLong="🔒".repeat(25); // 100 UTF-8 bytes but only 50 Java chars.
        mvc.perform(post("/register").with(csrf())
                .param("username","oversized-emoji-secret")
                .param("password",tooLong))
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("72 byte UTF-8")));
        assertThat(users.existsByUsername("oversized-emoji-secret")).isFalse();
    }

    @Test
    void permitsValidBcryptLengthAndHashesPassword() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("username","valid-registration-bytes")
                .param("password","validSecret123456"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));
        assertThat(users.findByUsername("valid-registration-bytes").orElseThrow().getPassword())
            .startsWith("$2");
    }
}
