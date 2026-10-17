package com.example.fbservice;

import com.example.fbservice.admin.AdminDashboardService;
import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:admindashboardtest;DB_CLOSE_DELAY=-1")
class AdminDashboardIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AdminDashboardService dashboard;
    @Autowired CampaignOrderService campaigns;

    @Test
    @WithMockUser(username="dashboard-admin",roles="ADMIN")
    void metricsUseRealCountsAndHideFromNonAdmin() throws Exception {
        long before=dashboard.metrics().totalCampaigns();
        campaigns.create("a-customer",new CampaignOrderRequest(SocialPlatform.YOUTUBE,
            CampaignServiceType.CONTENT_PLAN,"https://youtube.com/@channel",
            "Cần nội dung hữu ích cho kênh dạy tin học."));
        assertThat(dashboard.metrics().totalCampaigns()).isEqualTo(before+1);
        mvc.perform(get("/admin/dashboard")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tổng chiến dịch")));
        mvc.perform(get("/admin/dashboard").with(user("customer")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").with(user("staff").roles("STAFF")))
            .andExpect(status().isForbidden());
    }
}
