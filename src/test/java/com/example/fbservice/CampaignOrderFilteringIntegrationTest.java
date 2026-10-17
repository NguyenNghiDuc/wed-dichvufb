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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:campaignfiltering;DB_CLOSE_DELAY=-1")
class CampaignOrderFilteringIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder newOrder(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@growlab", "Kế hoạch nội dung hữu ích và hợp lệ."));
    }

    @Test
    @WithMockUser(username = "filter-alice")
    void apiFiltersByExactIdAndStatusWithinOwner() throws Exception {
        CampaignOrder first = newOrder("filter-alice");
        CampaignOrder second = newOrder("filter-alice");
        newOrder("filter-bob");

        mvc.perform(get("/api/v1/campaign-orders/page")
                .param("orderId", first.getId().toString())
                .param("status", "PENDING_REVIEW"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(first.getId()))
            .andExpect(jsonPath("$.content[0].ownerUsername").value("filter-alice"));

        mvc.perform(get("/api/v1/campaign-orders/page")
                .param("orderId", second.getId().toString())
                .param("status", "COMPLETED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/campaign-orders/page").param("status", "PENDING_REVIEW")
                .param("size", "1").param("page", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].ownerUsername").value("filter-alice"));
    }

    @Test
    @WithMockUser(username = "filter-owner")
    void filteringCannotLeakAnotherUsersOrderOrCounts() throws Exception {
        CampaignOrder privateOrder = newOrder("filter-private");
        newOrder("filter-owner");
        mvc.perform(get("/api/v1/campaign-orders/page")
                .param("orderId", privateOrder.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/v1/campaign-orders/page")
                .with(user("filter-outsider")).param("status", "PENDING_REVIEW"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/campaign-orders/" + privateOrder.getId()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "filter-invalid")
    void rejectsInvalidAndUnboundedFilters() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/page").param("orderId", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").param("orderId", "abc"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").param("status", "NOT_A_STATUS"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").param("size", "51"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").param("page", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").param("page", "10001"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "filter-web")
    void webFilterPreservesSelectionAndRejectsInvalidInputs() throws Exception {
        CampaignOrder order = newOrder("filter-web");
        mvc.perform(get("/campaigns").param("orderId", order.getId().toString())
                .param("status", "PENDING_REVIEW"))
            .andExpect(status().isOk())
            .andExpect(view().name("campaign_list"))
            .andExpect(model().attribute("orderId", order.getId()))
            .andExpect(model().attribute("selectedStatus", CampaignStatus.PENDING_REVIEW))
            .andExpect(content().string(containsString("name=\"orderId\"")))
            .andExpect(content().string(containsString("name=\"status\"")));
        mvc.perform(get("/campaigns").param("orderId", "-5"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns").param("status", "NOT_A_STATUS"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns").with(user("filter-web-other"))
                .param("orderId", order.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders", org.hamcrest.Matchers.empty()));
    }

    @Test
    void anonymousCannotSearchOrders() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/page").param("status", "PENDING_REVIEW"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/campaigns").param("orderId", "1"))
            .andExpect(status().is3xxRedirection());
    }
}
