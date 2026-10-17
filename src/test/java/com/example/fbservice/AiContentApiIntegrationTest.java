package com.example.fbservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties={
    "spring.datasource.url=jdbc:h2:mem:aiapibounded;DB_CLOSE_DELAY=-1",
    "growth.ai.api-key="
})
class AiContentApiIntegrationTest {
    @Autowired MockMvc mvc;
    private static final String BASE_JSON=
        "{\"platform\":\"TIKTOK\",\"topic\":\"study skills\",\"audience\":\"college students\"}";

    @Test @WithMockUser(username="ai-api-validation")
    void malformedAndUnboundedInputIsRejectedBeforeCallingProvider() throws Exception {
        mvc.perform(post("/api/v1/ai/generate").with(csrf()).contentType("application/json")
                .content("{\"platform\":\"TIKTOK\",\"topic\":\"study tips\","
                  +"\"audience\":\"students\",\"task\":\"CONTENT_PLAN\",\"days\":31}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/ai/generate").with(csrf()).contentType("application/json")
                .content("{\"platform\":\"TIKTOK\",\"topic\":\"study tips\","
                  +"\"audience\":\"students\",\"task\":\"UNKNOWN\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/ai/generate").with(csrf()).contentType("application/json")
                .content("{\"platform\":\"TIKTOK\",\"topic\":\"study tips\","
                  +"\"audience\":\"students\",\"language\":\"UNKNOWN\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/ai/generate").with(csrf()).contentType("application/json")
                .content("{\"platform\":\"TIKTOK\",\"topic\":\"x\",\"audience\":\"students\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/ai/generate").with(csrf()).contentType("application/json")
                .content(BASE_JSON))
            .andExpect(status().isServiceUnavailable());
    }

    @Test @WithMockUser(username="ai-capability-owner-29")
    void capabilityStatusDoesNotExposeSecretsOrSuggestGlobalCostAccounting() throws Exception {
        mvc.perform(get("/api/v1/ai/capabilities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("LOCAL_TEMPLATES_ONLY"))
            .andExpect(jsonPath("$.providerConfigured").value(false))
            .andExpect(jsonPath("$.quotaScope").value("PER_INSTANCE_ONLY"))
            .andExpect(jsonPath("$.perUserPerMinute").value(5))
            .andExpect(jsonPath("$.perUserPer24Hours").value(25))
            .andExpect(jsonPath("$.publishingEnabled").value(false))
            .andExpect(jsonPath("$.supportedTasks.length()").value(7));
    }

    @Test @WithMockUser(username="ai-api-platform-29")
    void mismatchedPlatformAndMissingRewriteDraftReturn400() throws Exception {
        mvc.perform(post("/api/v1/ai/generate").with(csrf()).contentType("application/json")
                .content("{\"platform\":\"FACEBOOK\",\"topic\":\"study tips\","
                  +"\"audience\":\"students\",\"task\":\"TIKTOK_IDEAS\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/ai/generate").with(csrf()).contentType("application/json")
                .content("{\"platform\":\"TIKTOK\",\"topic\":\"study tips\","
                  +"\"audience\":\"students\",\"task\":\"YOUTUBE_SHORTS\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/ai/generate").with(csrf()).contentType("application/json")
                .content("{\"platform\":\"YOUTUBE\",\"topic\":\"study tips\","
                  +"\"audience\":\"students\",\"task\":\"OPTIMIZE\",\"existingContent\":\" \"}"))
            .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username="ai-template-fallback-29")
    void existingTemplateFallbackIsExplicitlyNonAiAndDoesNotNeedCredentials() throws Exception {
        mvc.perform(get("/api/v1/content-suggestions")
                .param("platform","TIKTOK").param("topic","study tips"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("TEMPLATE_ONLY"))
            .andExpect(jsonPath("$.platform").value("TIKTOK"))
            .andExpect(jsonPath("$.suggestions.length()").value(3));
        mvc.perform(get("/api/v1/content-suggestions")
                .param("platform","TIKTOK").param("topic","z".repeat(101)))
            .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username="ai-api-rate-budget-29")
    void rateLimitStopsExcessCallsBeforeProvider() throws Exception {
        for(int i=0;i<5;i++) {
            mvc.perform(post("/api/v1/ai/generate").with(csrf()).contentType("application/json")
                    .content(BASE_JSON))
                .andExpect(status().isServiceUnavailable());
        }
        mvc.perform(post("/api/v1/ai/generate").with(csrf()).contentType("application/json")
                .content(BASE_JSON))
            .andExpect(status().isTooManyRequests());
    }

    @Test @WithMockUser(username="ai-api-csrf-29")
    void aiRequestsRequireCsrf() throws Exception {
        mvc.perform(post("/api/v1/ai/generate").contentType("application/json")
                .content(BASE_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUsersCannotGenerateAiContent() throws Exception {
        mvc.perform(post("/api/v1/ai/generate").with(csrf())
                .contentType("application/json").content(BASE_JSON))
            .andExpect(status().is3xxRedirection());
    }
}
