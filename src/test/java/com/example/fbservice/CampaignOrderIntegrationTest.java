package com.example.fbservice;

import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:growlabtest;DB_CLOSE_DELAY=-1")
class CampaignOrderIntegrationTest {
    @Autowired private MockMvc mvc;

    @Test
    void anonymousCannotSeeCampaigns() throws Exception {
        mvc.perform(get("/campaigns")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "alice")
    void createAndReadOnlyOwnOrder() throws Exception {
        String json = """
            {"platform":"TIKTOK","serviceType":"CONTENT_PLAN",
             "targetUrl":"https://www.tiktok.com/@sample",
             "brief":"Lên lịch nội dung cho kênh bán hàng trong tuần."}
            """;
        var result = mvc.perform(post("/api/v1/campaign-orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quotedPriceVnd").value(149000))
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
            .andReturn();
        String location = result.getResponse().getHeader("Location");
        mvc.perform(get(location)).andExpect(status().isOk())
            .andExpect(jsonPath("$.ownerUsername").value("alice"));
        mvc.perform(get(location).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("bob")))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice")
    void rejectWrongPlatformAndMissingCsrf() throws Exception {
        String invalid = """
            {"platform":"TIKTOK","serviceType":"PROFILE_AUDIT",
             "targetUrl":"https://example.com/other",
             "brief":"Đánh giá tài khoản bán hàng hợp lệ."}
            """;
        mvc.perform(post("/api/v1/campaign-orders").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(invalid))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/campaign-orders")
            .contentType(MediaType.APPLICATION_JSON).content(invalid))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice")
    void normalUserCannotAccessAdmin() throws Exception {
        mvc.perform(get("/admin/campaign-orders")).andExpect(status().isForbidden());
    }
}
