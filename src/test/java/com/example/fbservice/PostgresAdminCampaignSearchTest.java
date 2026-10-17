package com.example.fbservice;

import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
    "spring.datasource.username=growlab",
    "spring.datasource.password=testpass",
    "server.servlet.session.cookie.secure=false"
})
class PostgresAdminCampaignSearchTest {
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder create(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.PROFILE_AUDIT,
            "https://www.youtube.com/@growlab", "Phân tích kênh và đề xuất nội dung hữu ích."));
    }

    @Test
    void adminCriteriaFiltersCorrectlyOnPostgres() {
        CampaignOrder a = create("pg-admin-search-a");
        CampaignOrder b = create("pg-admin-search-b");
        campaigns.updateStatusForAdmin(b.getId(), CampaignStatus.APPROVED, "pg-admin-search-admin");

        assertThat(campaigns.pageForAdmin(0, a.getId(), CampaignStatus.PENDING_REVIEW,
                "pg-admin-search-a").getTotalElements()).isEqualTo(1);
        assertThat(campaigns.pageForAdmin(0, b.getId(), CampaignStatus.PENDING_REVIEW,
                "pg-admin-search-b").getTotalElements()).isZero();
        assertThat(campaigns.pageForAdmin(0, b.getId(), CampaignStatus.APPROVED,
                "pg-admin-search-b").getContent())
                .extracting(CampaignOrder::getId).containsExactly(b.getId());
        assertThat(campaigns.pageForAdmin(0, null, null,
                "pg-admin-search-a").getContent())
                .extracting(CampaignOrder::getId).contains(a.getId()).doesNotContain(b.getId());
    }
}
