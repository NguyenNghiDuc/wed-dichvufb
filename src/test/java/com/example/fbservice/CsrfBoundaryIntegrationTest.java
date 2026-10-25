package com.example.fbservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties={
    "spring.datasource.url=jdbc:h2:mem:csrfregression;DB_CLOSE_DELAY=-1",
    "growth.payments.webhook-secret=test-only-local-nonproduction-signing-key-123456789"
})
class CsrfBoundaryIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void publicLoginAndRegistrationFormsIncludeCsrfTokens() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/register")).andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void loginAndRegistrationPostsRequireCsrfEvenWhenPubliclyAccessible() throws Exception {
        mvc.perform(post("/register").param("username", "newtestuser")
                .param("password", "not-a-real-password"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/register").with(csrf()).param("username", "xx")
                .param("password", "not-a-real-password"))
            .andExpect(status().isOk())
            .andExpect(view().name("register"));
        mvc.perform(post("/login").param("username", "unknown-csrf-test")
                .param("password", "incorrect"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/login").with(csrf()).param("username", "unknown-csrf-test")
                .param("password", "incorrect"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void invalidCsrfTokenCannotRegisterOrAuthenticate() throws Exception {
        String username = "csrf-invalid-token-user";
        mvc.perform(post("/register").with(csrf().useInvalidToken())
                .param("username", username)
                .param("password", "not-a-real-password"))
            .andExpect(status().isSeeOther())
            .andExpect(redirectedUrl("/register?sessionExpired"));
        org.assertj.core.api.Assertions.assertThat(users.existsByUsername(username)).isFalse();

        mvc.perform(post("/login").with(csrf().useInvalidToken())
                .param("username", username)
                .param("password", "not-a-real-password"))
            .andExpect(status().isSeeOther())
            .andExpect(redirectedUrl("/login?sessionExpired"));
    }

    @Test
    @WithMockUser(username = "csrf-owner", roles = "ADMIN")
    void authenticatedStateChangesRequireCsrf() throws Exception {
        mvc.perform(post("/admin/campaign-orders/1/status").param("status", "APPROVED"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/campaign-orders/1/cancel"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/logout"))
            .andExpect(status().isForbidden());
    }

    @Test
    void onlySignedWebhookPostIsCsrfExemptAndHmacIsStillEnforced() throws Exception {
        mvc.perform(post("/api/v1/payments/webhooks/internal")
                .contentType(MediaType.APPLICATION_JSON).content("{}")
                .header("X-GrowLab-Signature", "0".repeat(64)))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/payments/webhooks/internal")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/payments/webhooks/internal"))
            .andExpect(status().isForbidden());
    }
}
