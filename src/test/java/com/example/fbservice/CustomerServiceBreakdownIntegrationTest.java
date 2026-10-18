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
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:servicebreakdown;DB_CLOSE_DELAY=-1")
class CustomerServiceBreakdownIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService orders;

    private void create(String owner, CampaignServiceType service) {
        orders.create(owner, new CampaignOrderRequest(SocialPlatform.YOUTUBE, service,
            "https://youtube.com/@channel", "Kế hoạch nội dung có giá trị cho kênh thật"));
    }

    @Test @WithMockUser(username="breakdown-owner")
    void countsOnlyAuthenticatedOwnerAndIgnoresCurrentListFilter() throws Exception {
        create("breakdown-owner", CampaignServiceType.CONTENT_PLAN);
        create("breakdown-owner", CampaignServiceType.CONTENT_PLAN);
        create("breakdown-owner", CampaignServiceType.PROFILE_AUDIT);
        create("breakdown-other", CampaignServiceType.AD_STRATEGY);
        mvc.perform(get("/api/v1/campaign-orders/summary/services"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].serviceType", hasItems("CONTENT_PLAN","PROFILE_AUDIT")))
            .andExpect(jsonPath("$[*].count", hasItems(2,1)));
        mvc.perform(get("/campaigns").param("status","COMPLETED"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Đơn hàng theo dịch vụ")));
        mvc.perform(get("/api/v1/campaign-orders/summary/services")
                .with(user("breakdown-other")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].serviceType").value("AD_STRATEGY"))
            .andExpect(jsonPath("$[0].count").value(1));
        mvc.perform(get("/api/v1/campaign-orders/summary/services")
                .with(user("breakdown-empty")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void anonymousCannotAccessCustomerBreakdown() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/summary/services"))
            .andExpect(status().is3xxRedirection());
    }
}
