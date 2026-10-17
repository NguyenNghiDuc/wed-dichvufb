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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:attentionowner;DB_CLOSE_DELAY=-1")
class CustomerCampaignAttentionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService orders;

    @Test @WithMockUser(username="attention-owner")
    void readOnlyAttentionIsVisibleOnlyToOwner() throws Exception {
        CampaignOrder mine=orders.create("attention-owner",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@attention", "Nội dung hữu ích cho khán giả thật"));
        CampaignOrder foreign=orders.create("attention-other",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@attention-other", "Kế hoạch nội dung hợp lệ cho khách hàng"));
        String url="/api/v1/campaign-orders/" + mine.getId() + "/attention";
        mvc.perform(get(url)).andExpect(status().isOk())
            .andExpect(jsonPath("$.needsAttention").value(false))
            .andExpect(jsonPath("$.message").value(""));
        mvc.perform(get(url).with(user("attention-other"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/campaign-orders/" + foreign.getId() + "/attention"))
            .andExpect(status().isNotFound());
        orders.updateStatusForAdmin(mine.getId(), CampaignStatus.REJECTED, "trusted-admin");
        mvc.perform(get(url)).andExpect(status().isOk())
            .andExpect(jsonPath("$.needsAttention").value(true))
            .andExpect(jsonPath("$.overdue").value(false));
        mvc.perform(get("/campaigns")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Liên hệ hỗ trợ")));
    }

    @Test void anonymousCannotSeeAttention() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/1/attention"))
            .andExpect(status().is3xxRedirection());
    }
}
