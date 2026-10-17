package com.example.fbservice;

import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:adminsearchtest;DB_CLOSE_DELAY=-1")
class AdminCampaignSearchIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder newOrder(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@contentchannel", "Kế hoạch nội dung hữu ích cho người học."));
    }

    @Test
    @WithMockUser(username = "search-admin", roles = "ADMIN")
    void adminSearchCombinesOwnerExactIdAndStatus() throws Exception {
        CampaignOrder first = newOrder("search-first");
        CampaignOrder second = newOrder("search-second");
        campaigns.updateStatusForAdmin(second.getId(), CampaignStatus.APPROVED, "search-admin");

        var result = campaigns.pageForAdmin(0, first.getId(), CampaignStatus.PENDING_REVIEW, "search-first");
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(CampaignOrder::getId).containsExactly(first.getId());

        mvc.perform(get("/admin/campaign-orders").param("owner", "search-second")
                .param("status", "APPROVED").param("orderId", second.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin_campaigns"))
            .andExpect(model().attribute("orders", hasSize(1)))
            .andExpect(model().attribute("selectedStatus", CampaignStatus.APPROVED))
            .andExpect(model().attribute("owner", "search-second"))
            .andExpect(content().string(containsString("name=\"owner\"")));

        mvc.perform(get("/admin/campaign-orders").param("owner", "search-first")
                .param("status", "APPROVED"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders", empty()));
        mvc.perform(get("/admin/campaign-orders").param("owner", "search"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders", empty()));
    }

    @Test
    @WithMockUser(username = "search-admin-2", roles = "ADMIN")
    void invalidFiltersAreRejectedWithoutExpensiveQueries() throws Exception {
        mvc.perform(get("/admin/campaign-orders").param("page", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/campaign-orders").param("page", "10001"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/campaign-orders").param("orderId", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/campaign-orders").param("orderId", "invalid"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/campaign-orders").param("status", "UNKNOWN"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/campaign-orders").param("owner", "%"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/campaign-orders").param("owner", "a".repeat(101)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void customerStaffAndAnonymousCannotSearchAdminOrders() throws Exception {
        mvc.perform(get("/admin/campaign-orders")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/campaign-orders").with(user("ordinary")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/campaign-orders").with(user("worker").roles("STAFF")))
            .andExpect(status().isForbidden());
    }
}
