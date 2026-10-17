package com.example.fbservice;

import com.example.fbservice.campaign.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:customerCsvExport;DB_CLOSE_DELAY=-1")
class CustomerCampaignCsvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder newOrder(String owner, String brief) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@growlab", brief));
    }

    @Test
    @WithMockUser(username = "customer-export-owner")
    void exportsOnlyOwnOrdersWithoutUsernameAndMitigatesSpreadsheetFormulas() throws Exception {
        CampaignOrder mine = newOrder("customer-export-owner", "=SUM(1,1) owner-only note");
        CampaignOrder others = newOrder("customer-export-private", "PRIVATE-SECRET customer message");

        String csv = mvc.perform(get("/campaigns/export.csv"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"my-campaigns.csv\""))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\uFEFFid,platform,service,status,price_vnd,created_at,target_url,brief\r\n");
        assertThat(csv).contains("\"" + mine.getId() + "\"").contains("'=SUM(1,1)");
        assertThat(csv).doesNotContain("PRIVATE-SECRET").doesNotContain("\"" + others.getId() + "\"");
        assertThat(csv).doesNotContain("owner_username").doesNotContain("customer-export-private");
        mvc.perform(get("/campaigns/export.csv").with(user("customer-export-outsider")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("owner-only note"))));
    }

    @Test
    @WithMockUser(username = "customer-export-filter")
    void respectsStatusAndExactOrderIdAndProvidesDownloadLink() throws Exception {
        CampaignOrder pending = newOrder("customer-export-filter", "Pending record test only");
        CampaignOrder cancelled = newOrder("customer-export-filter", "Cancelled record test only");
        campaigns.cancelForOwner("customer-export-filter", cancelled.getId());

        String csv = mvc.perform(get("/campaigns/export.csv")
                .param("orderId", cancelled.getId().toString())
                .param("status", "CANCELLED"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("Cancelled record test only").doesNotContain("Pending record test only");

        mvc.perform(get("/campaigns/export.csv")
                .param("orderId", cancelled.getId().toString())
                .param("status", "PENDING_REVIEW"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("Cancelled record test only"))));

        mvc.perform(get("/campaigns").param("status", "CANCELLED"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/campaigns/export.csv")));
    }

    @Test
    @WithMockUser(username = "customer-export-invalid")
    void rejectsInvalidFilters() throws Exception {
        mvc.perform(get("/campaigns/export.csv").param("orderId", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns/export.csv").param("orderId", "not-a-number"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns/export.csv").param("status", "INVALID_STATUS"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousCannotDownloadCampaignHistory() throws Exception {
        mvc.perform(get("/campaigns/export.csv"))
            .andExpect(status().is3xxRedirection());
    }
}
