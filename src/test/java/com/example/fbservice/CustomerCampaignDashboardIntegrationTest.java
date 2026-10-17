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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerdashboard;DB_CLOSE_DELAY=-1")
class CustomerCampaignDashboardIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder create(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@growlab", "Lập kế hoạch nội dung giáo dục có ích."));
    }

    @Test
    @WithMockUser(username="dashboard-owner")
    void summaryAndCardsCountOnlyAuthenticatedOwnersOrders() throws Exception {
        create("dashboard-owner");
        CampaignOrder approved = create("dashboard-owner");
        CampaignOrder completed = create("dashboard-owner");
        create("dashboard-other");
        campaigns.updateStatusForAdmin(approved.getId(), CampaignStatus.APPROVED, "dashboard-admin");
        campaigns.updateStatusForAdmin(completed.getId(), CampaignStatus.APPROVED, "dashboard-admin");
        campaigns.updateStatusForAdmin(completed.getId(), CampaignStatus.IN_PROGRESS, "dashboard-admin");
        campaigns.updateStatusForAdmin(completed.getId(), CampaignStatus.COMPLETED, "dashboard-admin");

        // Caller-provided 'owner' has no effect on principal-scoped aggregation.
        mvc.perform(get("/api/v1/campaign-orders/summary").param("owner", "dashboard-other"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.pendingReview").value(1))
            .andExpect(jsonPath("$.approved").value(1))
            .andExpect(jsonPath("$.inProgress").value(0))
            .andExpect(jsonPath("$.completed").value(1))
            .andExpect(jsonPath("$.rejected").value(0))
            .andExpect(jsonPath("$.cancelled").value(0));

        mvc.perform(get("/api/v1/campaign-orders/summary").with(user("dashboard-other")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.pendingReview").value(1));
        mvc.perform(get("/api/v1/campaign-orders/summary").with(user("dashboard-empty")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0));

        mvc.perform(get("/campaigns").param("status", "COMPLETED"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("summary", new CampaignSummary(3,1,1,0,1,0,0)))
            .andExpect(content().string(containsString("Tổng chiến dịch")))
            .andExpect(content().string(containsString("Đang xử lý")))
            .andExpect(content().string(containsString("Đã hoàn thành")));
    }

    @Test
    void anonymousCannotReadAnyCustomerMetrics() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/summary"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/campaigns"))
            .andExpect(status().is3xxRedirection());
    }
}
