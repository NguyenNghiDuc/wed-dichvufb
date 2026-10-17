package com.example.fbservice;

import com.example.fbservice.campaign.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customeradvancedfilters;DB_CLOSE_DELAY=-1")
class CustomerCampaignAdvancedFiltersIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder create(String owner, CampaignServiceType type) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, type, "https://youtube.com/@growlab",
            "Kế hoạch nội dung hữu ích cho kênh này."));
    }

    @Test @WithMockUser(username="advanced-owner")
    void apiWebAndCsvShareTenantScopedServiceAndDateFilters() throws Exception {
        CampaignOrder included = create("advanced-owner", CampaignServiceType.CONTENT_PLAN);
        CampaignOrder excluded = create("advanced-owner", CampaignServiceType.PROFILE_AUDIT);
        CampaignOrder other = create("advanced-other", CampaignServiceType.CONTENT_PLAN);
        String today = LocalDate.now().toString();

        mvc.perform(get("/api/v1/campaign-orders/page")
                .param("serviceType", "CONTENT_PLAN").param("from", today).param("to", today))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(included.getId()));

        mvc.perform(get("/campaigns").param("serviceType", "CONTENT_PLAN")
                .param("from", today).param("to", today))
            .andExpect(status().isOk()).andExpect(view().name("campaign_list"))
            .andExpect(model().attribute("selectedServiceType", CampaignServiceType.CONTENT_PLAN))
            .andExpect(content().string(containsString("name=\"serviceType\"")));

        mvc.perform(get("/campaigns/export.csv")
                .param("serviceType", "CONTENT_PLAN").param("from", today).param("to", today))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().string(containsString("CONTENT_PLAN")))
            .andExpect(content().string(not(containsString("PROFILE_AUDIT"))))
            .andExpect(content().string(not(containsString("advanced-other"))));
        mvc.perform(get("/api/v1/campaign-orders/page")
                .with(user("advanced-other")).param("orderId", included.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/campaign-orders/page")
                .param("orderId", other.getId().toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/campaign-orders/page")
                .param("serviceType", "PROFILE_AUDIT"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(excluded.getId()));
    }

    @Test @WithMockUser(username="advanced-invalid")
    void invalidDatesAndEnumsFailClosed() throws Exception {
        String later = LocalDate.now().plusDays(1).toString();
        String earlier = LocalDate.now().minusDays(1).toString();
        mvc.perform(get("/api/v1/campaign-orders/page").param("from", later).param("to", earlier))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns").param("from", later).param("to", earlier))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns/export.csv").param("from", later).param("to", earlier))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").param("serviceType","INVENTED"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").param("from", "not-a-date"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").param("from", "1900-01-01"))
            .andExpect(status().isBadRequest());
    }

    @Test void anonymousCannotReadFilteredCustomerData() throws Exception {
        mvc.perform(get("/campaigns/export.csv").param("serviceType", "CONTENT_PLAN"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/v1/campaign-orders/page").param("serviceType", "CONTENT_PLAN"))
            .andExpect(status().is3xxRedirection());
    }
}
