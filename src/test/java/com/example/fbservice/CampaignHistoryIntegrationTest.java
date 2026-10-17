package com.example.fbservice;

import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:campaignhistoryapi;DB_CLOSE_DELAY=-1")
class CampaignHistoryIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private CampaignOrderService campaigns;

    private CampaignOrder newCampaign(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
                SocialPlatform.YOUTUBE, CampaignServiceType.PROFILE_AUDIT,
                "https://youtube.com/@growlab",
                "Phân tích tài khoản kênh giáo dục và kế hoạch nội dung."));
    }

    @Test
    @WithMockUser(username = "history-api-alice")
    void paginatesChronologicallyWithoutLeakingInternalActor() throws Exception {
        CampaignOrder order = newCampaign("history-api-alice");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.APPROVED, "internal-admin-username");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.IN_PROGRESS, "internal-admin-username");

        mvc.perform(get("/api/v1/campaign-orders/" + order.getId() + "/history")
                .param("size", "1").param("page", "0"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].status").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$.content[0].actor").doesNotExist());

        mvc.perform(get("/api/v1/campaign-orders/" + order.getId() + "/history")
                .param("size", "1").param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].previousStatus").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$.content[0].status").value("APPROVED"))
            .andExpect(jsonPath("$.content[0].actor").doesNotExist());
    }

    @Test
    @WithMockUser(username = "history-api-owner")
    void hidesForeignCampaignAndMissingCampaignWithSameNotFound() throws Exception {
        CampaignOrder privateOrder = newCampaign("history-api-other");
        mvc.perform(get("/api/v1/campaign-orders/" + privateOrder.getId() + "/history"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/campaign-orders/" + privateOrder.getId() + "/history")
                .with(user("history-api-other")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/campaign-orders/999999999/history"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "history-api-validation")
    void rejectsOutOfRangePaginationAndRequiresLogin() throws Exception {
        CampaignOrder order = newCampaign("history-api-validation");
        String endpoint = "/api/v1/campaign-orders/" + order.getId() + "/history";
        mvc.perform(get(endpoint).param("page", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get(endpoint).param("page", "10001"))
            .andExpect(status().isBadRequest());
        mvc.perform(get(endpoint).param("size", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get(endpoint).param("size", "51"))
            .andExpect(status().isBadRequest());
        mvc.perform(get(endpoint).with(user("history-api-stranger")))
            .andExpect(status().isNotFound());
    }

    @Test
    void anonymousMustSignIn() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/1/history"))
            .andExpect(status().is3xxRedirection());
    }
}
