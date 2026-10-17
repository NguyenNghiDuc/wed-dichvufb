package com.example.fbservice;

import com.example.fbservice.admin.AdminDashboardService;
import com.example.fbservice.campaign.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties={
    "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
    "spring.datasource.username=growlab",
    "spring.datasource.password=testpass",
    "server.servlet.session.cookie.secure=false"
})
class PostgresAdminCampaignAttentionTest {
    @Autowired CampaignOrderRepository orders;
    @Autowired AdminDashboardService dashboard;

    @Test
    void productionSchemaSupportsAgingMetricsWithoutMutatingOrders() {
        var before = dashboard.metrics();
        LocalDateTime now = LocalDateTime.now();
        CampaignOrder candidate = new CampaignOrder("attention-pg-review",
            new CampaignOrderRequest(SocialPlatform.YOUTUBE,
                CampaignServiceType.CONTENT_PLAN, "https://youtube.com/@growlab",
                "Kiểm tra nội dung phù hợp với chính sách."), 79000);
        ReflectionTestUtils.setField(candidate, "createdAt", now.minusHours(80));
        ReflectionTestUtils.setField(candidate, "updatedAt", now.minusHours(80));
        CampaignOrder stored = orders.save(candidate);
        var after = dashboard.metrics();
        assertThat(after.pendingNeedsReview()).isEqualTo(before.pendingNeedsReview() + 1);
        assertThat(orders.findById(stored.getId())).get()
            .extracting(CampaignOrder::getStatus).isEqualTo(CampaignStatus.PENDING_REVIEW);
    }
}
