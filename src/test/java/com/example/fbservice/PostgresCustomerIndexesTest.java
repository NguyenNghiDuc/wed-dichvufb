package com.example.fbservice;

import com.example.fbservice.campaign.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties={
    "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
    "spring.datasource.username=growlab",
    "spring.datasource.password=testpass",
    "server.servlet.session.cookie.secure=false"
})
class PostgresCustomerIndexesTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired CampaignOrderService orders;

    @Test void flywayCreatesCustomerIndexesAndOwnerScopedFiltersWork() {
        assertThat(jdbc.queryForObject(
            "select count(*) from pg_indexes where schemaname='public' and indexname='idx_campaign_owner_created_id'",
            Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "select count(*) from pg_constraint where conname='uk_campaign_review_campaign'",
            Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "select count(*) from information_schema.tables where table_schema='public' and table_name='customer_profiles'",
            Integer.class)).isEqualTo(1);

        CampaignOrder mine = orders.create("pg-index-owner-26", new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@realchannel", "Nội dung hợp pháp cho khách hàng thực"));
        CampaignOrder other = orders.create("pg-index-other-26", new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@anotherchannel", "Nội dung thực tế để phát triển kênh"));
        var page = orders.pageMine("pg-index-owner-26", 0, 20, null,
            CampaignStatus.PENDING_REVIEW, CampaignServiceType.CONTENT_PLAN,
            LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        assertThat(page.getContent()).extracting(CampaignOrder::getId)
            .contains(mine.getId()).doesNotContain(other.getId());
    }
}
