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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4trendcsv;DB_CLOSE_DELAY=-1")
class AdminOrderTrendCsvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    @Test @WithMockUser(username="w4-trend-csv-admin",roles="ADMIN")
    void csvMatchesCurrentTrendsAndNeverLeaksCustomerIdentity() throws Exception {
        CampaignOrder order=campaigns.create("private-csv-customer",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@truth", "Kế hoạch bài đăng phù hợp chính sách."));
        var json=mvc.perform(get("/api/v1/admin/analytics/campaign-trend").param("period","DAY"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var response=mvc.perform(get("/admin/reports/campaign-trend.csv").param("period","DAY"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("Content-Disposition",containsString("admin-campaign-trend-day.csv")))
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andReturn().getResponse();
        String csv=new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(json).contains("\"totalCreated\":1");
        assertThat(csv).startsWith("\uFEFFperiod_start,created_orders,currently_completed_orders");
        assertThat(csv.split("\r\n")).hasSize(15);
        assertThat(csv).doesNotContain("private-csv-customer","youtube.com","password");
        mvc.perform(get("/admin/dashboard").param("period","MONTH"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("campaign-trend.csv")));
        mvc.perform(get("/admin/reports/campaign-trend.csv").param("period","UNTRUSTED"))
            .andExpect(status().isBadRequest());
    }

    @Test void csvHasAdminOnlyAccess() throws Exception {
        mvc.perform(get("/admin/reports/campaign-trend.csv"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/reports/campaign-trend.csv")
            .with(user("w4-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/reports/campaign-trend.csv")
            .with(user("w4-client").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
