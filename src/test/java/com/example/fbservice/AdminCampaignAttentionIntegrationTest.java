package com.example.fbservice;

import com.example.fbservice.admin.AdminDashboardService;
import com.example.fbservice.campaign.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:attentiontest;DB_CLOSE_DELAY=-1")
class AdminCampaignAttentionIntegrationTest {
    @Autowired CampaignOrderRepository orders;
    @Autowired AdminDashboardService dashboard;
    @Autowired MockMvc mvc;

    private CampaignOrder order(String username, CampaignStatus status,
                                LocalDateTime created, LocalDateTime updated) {
        CampaignOrder value = new CampaignOrder(username,
            new CampaignOrderRequest(SocialPlatform.YOUTUBE,
                CampaignServiceType.PROFILE_AUDIT, "https://youtube.com/@growlab",
                "Rà soát nội dung và chiến dịch hợp lệ."), 79000);
        if (status == CampaignStatus.IN_PROGRESS) {
            value.changeStatus(CampaignStatus.APPROVED);
            value.changeStatus(CampaignStatus.IN_PROGRESS);
        }
        ReflectionTestUtils.setField(value, "createdAt", created);
        ReflectionTestUtils.setField(value, "updatedAt", updated);
        return orders.save(value);
    }

    @Test
    @WithMockUser(username = "attention-admin", roles = "ADMIN")
    void countsOnlyStalePendingAndInProgressOrders() throws Exception {
        var before = dashboard.metrics();
        LocalDateTime now = LocalDateTime.now();
        order("attention-pending-old", CampaignStatus.PENDING_REVIEW,
            now.minusHours(72), now.minusHours(72));
        order("attention-pending-new", CampaignStatus.PENDING_REVIEW,
            now.minusHours(3), now.minusHours(3));
        order("attention-progress-old", CampaignStatus.IN_PROGRESS,
            now.minusDays(10), now.minusDays(9));
        order("attention-progress-new", CampaignStatus.IN_PROGRESS,
            now.minusDays(9), now.minusHours(3));

        var after = dashboard.metrics();
        assertThat(after.pendingNeedsReview()).isEqualTo(before.pendingNeedsReview() + 1);
        assertThat(after.progressingNeedsReview()).isEqualTo(before.progressingNeedsReview() + 1);
        assertThat(after.pendingCampaigns()).isEqualTo(before.pendingCampaigns() + 2);

        mvc.perform(get("/admin/dashboard")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Chờ duyệt trên 48 giờ")))
            .andExpect(content().string(containsString("Đang xử lý trên 7 ngày")));
        mvc.perform(get("/admin/dashboard").with(user("attention-customer")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").with(user("attention-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
    }
}
