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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:contentplanfallback29;DB_CLOSE_DELAY=-1")
class ContentTemplatePlanApiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="template-owner-29")
    void servesNonAiTemplatesWithoutApiKeyOrPublicationRights() throws Exception {
        mvc.perform(get("/api/v1/content-templates/plan")
                .param("platform","YOUTUBE").param("topic","Study habits").param("days","30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("TEMPLATE_ONLY"))
            .andExpect(jsonPath("$.platform").value("YOUTUBE"))
            .andExpect(jsonPath("$.days").value(30))
            .andExpect(jsonPath("$.suggestions.length()").value(30))
            .andExpect(jsonPath("$.suggestions[0].day").value(1))
            .andExpect(jsonPath("$.suggestions[29].day").value(30));
    }

    @Test @WithMockUser(username="template-invalid-29")
    void rejectsInvalidParametersWith400() throws Exception {
        mvc.perform(get("/api/v1/content-templates/plan")
                .param("platform","YOUTUBE").param("topic","Study habits").param("days","31"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/content-templates/plan")
                .param("platform","UNKNOWN").param("topic","Study habits"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/content-templates/plan")
                .param("platform","TIKTOK").param("topic"," "))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousCannotReadTemplateGenerator() throws Exception {
        mvc.perform(get("/api/v1/content-templates/plan")
                .param("platform","TIKTOK").param("topic","Study habits"))
            .andExpect(status().is3xxRedirection());
    }
}
