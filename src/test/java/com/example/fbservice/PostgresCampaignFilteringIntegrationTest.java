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
class PostgresCampaignFilteringIntegrationTest {
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder create(String username) {
        return campaigns.create(username, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.PROFILE_AUDIT,
            "https://youtube.com/@growlab", "Đánh giá nội dung kênh theo chính sách."));
    }

    @Test
    void scopedCriteriaFiltersAndCountWorkOnRealPostgres() {
        CampaignOrder mine = create("pg-filter-a");
        CampaignOrder other = create("pg-filter-b");

        var matching = campaigns.pageMine("pg-filter-a", 0, 20,
                mine.getId(), CampaignStatus.PENDING_REVIEW);
        assertThat(matching.getTotalElements()).isEqualTo(1);
        assertThat(matching.getContent()).extracting(CampaignOrder::getId)
                .containsExactly(mine.getId());

        assertThat(campaigns.pageMine("pg-filter-a", 0, 20,
                other.getId(), CampaignStatus.PENDING_REVIEW).getTotalElements()).isZero();
        assertThat(campaigns.pageMine("pg-filter-a", 0, 20,
                mine.getId(), CampaignStatus.COMPLETED).getTotalElements()).isZero();
        assertThat(campaigns.pageMine("pg-filter-b", 0, 20,
                null, CampaignStatus.PENDING_REVIEW).getContent())
                .extracting(CampaignOrder::getId).contains(other.getId())
                .doesNotContain(mine.getId());
        // PostgreSQL LOCATE/STRPOS semantics: search must treat percent and
        // underscores as literal characters and never return another owner.
        CampaignOrder literal = campaigns.create("pg-filter-a",
            new CampaignOrderRequest(SocialPlatform.YOUTUBE,
                CampaignServiceType.PROFILE_AUDIT,
                "https://youtube.com/@literal_pg_filter",
                "Kế hoạch phát triển kênh 100% bền vững."));
        CampaignOrder forbidden = campaigns.create("pg-filter-b",
            new CampaignOrderRequest(SocialPlatform.YOUTUBE,
                CampaignServiceType.PROFILE_AUDIT,
                "https://youtube.com/@different_foreign",
                "Nội dung 100% riêng tư của người dùng khác."));
        var matches = campaigns.pageMine("pg-filter-a", 0, 20, null, null,
            null, null, null, "100%");
        assertThat(matches.getContent()).extracting(CampaignOrder::getId)
            .contains(literal.getId()).doesNotContain(forbidden.getId());
        var exactUnderscore = campaigns.pageMine("pg-filter-a", 0, 20, null, null,
            null, null, null, "_pg_filter");
        assertThat(exactUnderscore.getContent()).extracting(CampaignOrder::getId)
            .contains(literal.getId()).doesNotContain(other.getId());
        assertThat(campaigns.pageMine("pg-filter-a", 0, 20, null, null,
            null, null, null, "nonexistent-search-token").getTotalElements()).isZero();
    }
}
