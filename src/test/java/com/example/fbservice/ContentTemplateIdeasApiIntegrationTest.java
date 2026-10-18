package com.example.fbservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:contentideas29;DB_CLOSE_DELAY=-1")
class ContentTemplateIdeasApiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="ideas-valid-29")
    void canReturnBilingualReadOnlyOutlinesWithoutAnyApiKey() throws Exception {
        mvc.perform(get("/api/v1/content-templates/ideas")
                .param("platform","TIKTOK").param("topic","Healthy study techniques")
                .param("task","TIKTOK_IDEAS").param("language","EN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("TEMPLATE_ONLY"))
            .andExpect(jsonPath("$.platform").value("TIKTOK"))
            .andExpect(jsonPath("$.language").value("EN"))
            .andExpect(jsonPath("$.task").value("TIKTOK_IDEAS"))
            .andExpect(jsonPath("$.suggestions.length()").value(3));
        mvc.perform(get("/api/v1/content-templates/ideas")
                .param("platform","INSTAGRAM").param("topic","Study tips")
                .param("task","HASHTAGS").param("language","VI"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.suggestions.length()").value(5));
    }

    @Test @WithMockUser(username="ideas-invalid-29")
    void cannotGenerateWrongPlatformOrUnsupportedAndUnboundedTask() throws Exception {
        mvc.perform(get("/api/v1/content-templates/ideas")
                .param("platform","YOUTUBE").param("topic","Study tips")
                .param("task","TIKTOK_IDEAS"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/content-templates/ideas")
                .param("platform","YOUTUBE").param("topic","Study tips")
                .param("task","OPTIMIZE"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/content-templates/ideas")
                .param("platform","YOUTUBE").param("topic","x".repeat(101))
                .param("task","HEADLINES"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/content-templates/ideas")
                .param("platform","INVALID").param("topic","Study tips")
                .param("task","HEADLINES"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousUsersRequireLogin() throws Exception {
        mvc.perform(get("/api/v1/content-templates/ideas")
                .param("platform","YOUTUBE").param("topic","Study tips")
                .param("task","HEADLINES"))
            .andExpect(status().is3xxRedirection());
    }
}
