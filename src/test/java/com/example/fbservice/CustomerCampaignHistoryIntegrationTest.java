package com.example.fbservice;

import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerhistory;DB_CLOSE_DELAY=-1")
class CustomerCampaignHistoryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder create(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@growlab", "Xây dựng nội dung giáo dục có ích."));
    }

    @Test
    @WithMockUser(username="history-owner")
    void historyIsOwnerScopedPagedStableAndHidesInternalActor() throws Exception {
        CampaignOrder order = create("history-owner");
        CampaignOrder privateOrder = create("history-other");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.APPROVED, "internal-admin-secret");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.IN_PROGRESS, "internal-admin-secret");
        String url = "/api/v1/campaign-orders/" + order.getId() + "/history";

        mvc.perform(get(url).param("page", "0").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].newStatus").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$.content[1].newStatus").value("APPROVED"))
            .andExpect(jsonPath("$.content[0].actor").doesNotExist());
        mvc.perform(get(url).param("page", "1").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].newStatus").value("IN_PROGRESS"));
        mvc.perform(get(url).with(user("history-other")))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/campaign-orders/" + privateOrder.getId() + "/history"))
            .andExpect(status().isNotFound());
        mvc.perform(get(url).param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get(url).param("size", "51")).andExpect(status().isBadRequest());
        mvc.perform(get(url).param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get(url).param("page", "10001")).andExpect(status().isBadRequest());

        mvc.perform(get("/campaigns/" + order.getId()))
            .andExpect(status().isOk())
            .andExpect(view().name("campaign_details"))
            .andExpect(content().string(containsString("Lịch sử xử lý")))
            .andExpect(content().string(containsString("Báo giá dự kiến")));
        mvc.perform(get("/campaigns/" + order.getId()).param("page", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns/" + privateOrder.getId()))
            .andExpect(status().isNotFound());
    }

    @Test
    void anonymousCannotReadHistory() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/1/history"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/campaigns/1"))
            .andExpect(status().is3xxRedirection());
    }
}
