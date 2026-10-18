package com.example.fbservice;

import com.example.fbservice.campaign.*;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:campaignMonthlyTrendW2;DB_CLOSE_DELAY=-1")
class CustomerCampaignMonthlyTrendIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder create(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@campaignmonthly",
            "Xây dựng nội dung giáo dục cho kênh lâu dài."));
    }

    @Test @WithMockUser(username="monthly-owner")
    void returnsSixOrderedMonthlyBucketsWithoutMixingAccounts() throws Exception {
        CampaignOrder recent = create("monthly-owner");
        CampaignOrder older = create("monthly-owner");
        CampaignOrder foreign = create("monthly-foreign");
        YearMonth twoMonthsAgo = YearMonth.now().minusMonths(2);
        jdbc.update("update campaign_orders set created_at=? where id=?",
            Timestamp.valueOf(twoMonthsAgo.atDay(1).atTime(12, 30)), older.getId());

        List<CampaignMonthlyCount> trend = campaigns.monthlyCountsForOwner("monthly-owner");
        assertThat(trend).hasSize(6);
        assertThat(trend.get(3).month()).isEqualTo(twoMonthsAgo.toString());
        assertThat(trend.get(3).orders()).isEqualTo(1);
        assertThat(trend.get(5).month()).isEqualTo(YearMonth.now().toString());
        assertThat(trend.get(5).orders()).isEqualTo(1);
        assertThat(trend.stream().mapToLong(CampaignMonthlyCount::orders).sum()).isEqualTo(2);
        assertThat(campaigns.monthlyCountsForOwner("monthly-foreign").get(5).orders()).isEqualTo(1);
        assertThat(campaigns.monthlyCountsForOwner("monthly-outsider").stream()
            .mapToLong(CampaignMonthlyCount::orders).sum()).isZero();

        mvc.perform(get("/api/v1/campaign-orders/summary/monthly"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(jsonPath("$.length()").value(6))
            .andExpect(jsonPath("$[3].orders").value(1))
            .andExpect(jsonPath("$[5].orders").value(1));
        mvc.perform(get("/api/v1/campaign-orders/summary/monthly")
                .with(user("monthly-outsider")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[5].orders").value(0));
        mvc.perform(get("/campaigns"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("monthlyCounts", trend))
            .andExpect(content().string(containsString("Xu hướng chiến dịch 6 tháng")));
        var csv = mvc.perform(get("/campaigns/summary/monthly.csv"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"my-campaign-monthly.csv\""))
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFmonth,orders\r\n")
            .contains("\"" + twoMonthsAgo + "\",\"1\"")
            .contains("\"" + YearMonth.now() + "\",\"1\"");
        assertThat(csv.split("\r\n")).hasSize(7); // header and exactly six calendar months
        var unrelatedCsv = mvc.perform(get("/campaigns/summary/monthly.csv")
                .with(user("monthly-outsider")))
            .andExpect(status().isOk()).andReturn()
            .getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(unrelatedCsv).doesNotContain(",\"1\"");
        assertThat(recent.getId()).isNotEqualTo(foreign.getId());
    }

    @Test void anonymousCannotReadMonthlyCounts() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/summary/monthly"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/campaigns/summary/monthly.csv"))
            .andExpect(status().is3xxRedirection());
    }
}
