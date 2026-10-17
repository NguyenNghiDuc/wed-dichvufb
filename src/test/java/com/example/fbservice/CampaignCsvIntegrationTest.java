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
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:campaigncsvtest;DB_CLOSE_DELAY=-1")
class CampaignCsvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    @Test
    @WithMockUser(username = "reporting-admin", roles = "ADMIN")
    void exportsOnlyToAdminAndEscapesUntrustedFormula() throws Exception {
        campaigns.create("customer-csv", new CampaignOrderRequest(
            SocialPlatform.TIKTOK, CampaignServiceType.CONTENT_PLAN,
            "https://tiktok.com/@sample",
            "=SUM(1,1) malicious spreadsheet formula"));
        mvc.perform(get("/admin/campaign-orders/export.csv"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"campaign-orders.csv\""))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("'=SUM(1,1)")));
        mvc.perform(get("/admin/campaign-orders/export.csv").with(user("normal-user")))
            .andExpect(status().isForbidden());
    }
}
