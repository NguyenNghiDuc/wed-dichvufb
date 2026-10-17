package com.example.fbservice;

import com.example.fbservice.admin.*;
import com.example.fbservice.campaign.*;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4analytics;DB_CLOSE_DELAY=-1")
class AdminAnalyticsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AdminDashboardService dashboard;
    @Autowired CampaignOrderService campaigns;
    @Autowired CampaignOrderRepository orders;

    private CampaignOrder newOrder(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@growlab", "Lên kế hoạch nội dung cộng đồng hợp lệ."));
    }

    @Test
    @WithMockUser(username="w4-analytics-admin",roles="ADMIN")
    void dashboardAggregatesRealQuotesStatusesAndStaffWorkload() throws Exception {
        var before = dashboard.operations();
        CampaignOrder pending = newOrder("w4-analytic-customer");
        CampaignOrder approved = newOrder("w4-analytic-customer2");
        campaigns.updateStatusForAdmin(approved.getId(), CampaignStatus.APPROVED, "w4-analytics-admin");
        approved.assignTo("w4-analytics-staff");
        orders.saveAndFlush(approved);
        var after = dashboard.operations();

        assertThat(after.pipelineQuotedValueVnd())
            .isEqualTo(before.pipelineQuotedValueVnd() +
                pending.getQuotedPriceVnd() + approved.getQuotedPriceVnd());
        assertThat(after.statuses()).anySatisfy(s -> {
            if (s.status() == CampaignStatus.PENDING_REVIEW) {
                assertThat(s.orderCount()).isGreaterThanOrEqualTo(1);
            }
        });
        assertThat(after.staffLoad()).anySatisfy(s -> {
            if (s.username().equals("w4-analytics-staff")) {
                assertThat(s.activeOrders()).isGreaterThanOrEqualTo(1);
            }
        });
        mvc.perform(get("/admin/dashboard"))
            .andExpect(status().isOk()).andExpect(view().name("admin_dashboard"))
            .andExpect(model().attributeExists("operations"))
            .andExpect(content().string(containsString("Không phải doanh thu")));
    }

    @Test
    @WithMockUser(username="w4-report-admin",roles="ADMIN")
    void aggregatedCsvHasNoPiiOrClaimOfSettledRevenue() throws Exception {
        mvc.perform(get("/admin/reports/overview.csv"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Content-Disposition", containsString("attachment")))
            .andExpect(content().string(containsString("active_pipeline_quotes_vnd")))
            .andExpect(content().string(not(containsString("owner_username"))))
            .andExpect(content().string(not(containsString("w4-analytic-customer"))));
    }

    @Test
    void nonAdminsCannotViewMetricsOrExportSummary() throws Exception {
        mvc.perform(get("/admin/reports/overview.csv")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/reports/overview.csv").with(user("w4-regular")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/reports/overview.csv").with(user("w4-employee").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").with(user("w4-employee").roles("STAFF")))
            .andExpect(status().isForbidden());
    }
}
