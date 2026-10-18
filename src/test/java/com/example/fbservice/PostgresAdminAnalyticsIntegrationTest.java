package com.example.fbservice;

import com.example.fbservice.admin.*;
import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties={
    "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
    "spring.datasource.username=growlab",
    "spring.datasource.password=testpass",
    "server.servlet.session.cookie.secure=false"
})
class PostgresAdminAnalyticsIntegrationTest {
    @Autowired AdminDashboardService dashboard;
    @Autowired CampaignOrderService campaigns;

    @Test
    void postgresGroupByReturnsValidStatusCountsAndQuotedAmounts() {
        CampaignOrder order = campaigns.create("w4-pg-analytics",
            new CampaignOrderRequest(SocialPlatform.YOUTUBE,
                CampaignServiceType.CONTENT_PLAN,"https://youtube.com/@growlab",
                "Đề xuất nội dung đúng quy định." ));
        var summary = dashboard.operations();
        assertThat(summary.statuses())
            .anySatisfy(s -> {
                if (s.status() == CampaignStatus.PENDING_REVIEW) {
                    assertThat(s.quotedValueVnd()).isGreaterThanOrEqualTo(order.getQuotedPriceVnd());
                }
            });
        assertThat(summary.pipelineQuotedValueVnd()).isGreaterThanOrEqualTo(order.getQuotedPriceVnd());
    }
}
