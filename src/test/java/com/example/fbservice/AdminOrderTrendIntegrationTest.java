package com.example.fbservice;

import com.example.fbservice.admin.AdminOrderTrendService;
import com.example.fbservice.campaign.*;
import java.time.LocalDate;
import java.sql.Timestamp;
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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4ordertrend;DB_CLOSE_DELAY=-1")
class AdminOrderTrendIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CampaignOrderService campaigns;
    @Autowired AdminOrderTrendService trend;

    private CampaignOrder createdOn(LocalDate day, boolean completed) {
        CampaignOrder order = campaigns.create("w4-trend-owner",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@channel", "Nội dung phù hợp pháp luật và chính sách."));
        if (completed) {
            campaigns.updateStatusForAdmin(order.getId(),CampaignStatus.APPROVED,"trusted-admin");
            campaigns.updateStatusForAdmin(order.getId(),CampaignStatus.IN_PROGRESS,"trusted-admin");
            campaigns.updateStatusForAdmin(order.getId(),CampaignStatus.COMPLETED,"trusted-admin");
        }
        jdbc.update("update campaign_orders set created_at=? where id=?",
            Timestamp.valueOf(day.atTime(12,0)), order.getId());
        return order;
    }

    @Test @WithMockUser(username="w4-trend-admin",roles="ADMIN")
    void dayWeekMonthAggregatesIncludeEmptyBucketsAndNeverClaimPaidRevenue() throws Exception {
        LocalDate today=LocalDate.now();
        createdOn(today.minusDays(1),false);
        createdOn(today.minusDays(1),true);
        createdOn(today,true);
        var day=trend.find(AdminOrderTrendService.Period.DAY,today);
        assertThat(day.buckets()).hasSize(14);
        assertThat(day.totalCreated()).isEqualTo(3);
        assertThat(day.buckets().get(12).created()).isEqualTo(2);
        assertThat(day.buckets().get(12).completed()).isEqualTo(1);
        assertThat(day.buckets().get(13).created()).isEqualTo(1);
        assertThat(trend.find(AdminOrderTrendService.Period.WEEK,today).buckets()).hasSize(12);
        assertThat(trend.find(AdminOrderTrendService.Period.MONTH,today).buckets()).hasSize(12);

        mvc.perform(get("/admin/dashboard").param("period","WEEK"))
            .andExpect(status().isOk()).andExpect(view().name("admin_dashboard"))
            .andExpect(content().string(containsString("Đơn hàng theo thời gian")))
            .andExpect(content().string(containsString("không phải doanh thu đã thu")));
        mvc.perform(get("/api/v1/admin/analytics/campaign-trend").param("period","DAY"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.period").value("DAY"))
            .andExpect(jsonPath("$.buckets.length()").value(14))
            .andExpect(jsonPath("$.totalCreated").value(3));
        mvc.perform(get("/api/v1/admin/analytics/campaign-trend").param("period","UNTRUSTED"))
            .andExpect(status().isBadRequest());
    }

    @Test void trendDataHasAdminOnlyPermissions() throws Exception {
        mvc.perform(get("/api/v1/admin/analytics/campaign-trend"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/v1/admin/analytics/campaign-trend")
            .with(user("w4-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").with(user("w4-customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
