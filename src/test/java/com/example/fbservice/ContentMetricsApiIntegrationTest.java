package com.example.fbservice;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:contentmetrics29;DB_CLOSE_DELAY=-1")
class ContentMetricsApiIntegrationTest {
    @Autowired MockMvc mvc;

    private String sampleJson(long views,long likes) {
        String published=LocalDateTime.now(ZoneOffset.UTC).minusDays(5).toString();
        return "{\"posts\":[{\"platform\":\"TIKTOK\",\"publishedAtUtc\":\""+published
            +"\",\"views\":"+views+",\"likes\":"+likes
            +",\"comments\":0,\"shares\":0,\"saves\":0}]}";
    }

    @Test @WithMockUser(username="metric-owner-29")
    void previewIsUnverifiedAndOnlyCalculatesFromExplicitInput() throws Exception {
        mvc.perform(post("/api/v1/content-analytics/preview").with(csrf())
                .contentType("application/json").content(sampleJson(100,10)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.source").value("USER_SUPPLIED_UNVERIFIED"))
            .andExpect(jsonPath("$.postCount").value(1))
            .andExpect(jsonPath("$.totalInteractions").value(10))
            .andExpect(jsonPath("$.totalViews").value(100))
            .andExpect(jsonPath("$.candidateHourUtc").value(nullValue()))
            .andExpect(jsonPath("$.caveat").isString());
    }

    @Test @WithMockUser(username="metric-invalid-29")
    void rejectsInvalidAndUnboundedInputs() throws Exception {
        mvc.perform(post("/api/v1/content-analytics/preview").with(csrf())
                .contentType("application/json").content("{\"posts\":[]}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/content-analytics/preview").with(csrf())
                .contentType("application/json").content(sampleJson(-2,10)))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/content-analytics/preview").with(csrf())
                .contentType("application/json").content(sampleJson(100,-5)))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/content-analytics/preview").with(csrf())
                .contentType("application/json").content("{\"posts\":[{\"platform\":\"UNKNOWN\"}]}"))
            .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username="metric-csrf-29")
    void rejectsStateChangingPostWithoutCsrfEvenThoughNoDataIsStored() throws Exception {
        mvc.perform(post("/api/v1/content-analytics/preview")
                .contentType("application/json").content(sampleJson(100,10)))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonymousMetricsRequestsRequireLogin() throws Exception {
        mvc.perform(post("/api/v1/content-analytics/preview").with(csrf())
                .contentType("application/json").content(sampleJson(100,10)))
            .andExpect(status().is3xxRedirection());
    }
}
