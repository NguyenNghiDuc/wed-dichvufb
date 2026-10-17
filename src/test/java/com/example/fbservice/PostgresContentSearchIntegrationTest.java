package com.example.fbservice;

import com.example.fbservice.campaign.SocialPlatform;
import com.example.fbservice.content.ContentDraftService;
import com.example.fbservice.content.DraftStatus;
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
class PostgresContentSearchIntegrationTest {
    @Autowired ContentDraftService drafts;

    @Test
    void postgresLiteralWildcardsOwnerIsolationAndFilters() {
        String owner="pg-content-w5-29";
        String other="pg-content-w5-other-29";
        drafts.save(owner,SocialPlatform.TIKTOK,"Sale 100%_today",
            "User supplied metrics require verification",null);
        drafts.save(owner,SocialPlatform.TIKTOK,"Sale 100xtoday",
            "Should not match literal wildcard",null);
        drafts.save(owner,SocialPlatform.YOUTUBE,"Sale 100%_today",
            "YouTube content",null);
        drafts.save(other,SocialPlatform.TIKTOK,"Sale 100%_today",
            "Not available to the first owner",null);

        var result=drafts.searchMine(owner,0,SocialPlatform.TIKTOK,DraftStatus.DRAFT,"100%_today");
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Sale 100%_today");
        assertThat(drafts.searchMine(other,0,SocialPlatform.YOUTUBE,null,"100%_today").getTotalElements()).isZero();
        assertThat(drafts.searchMine(owner,0,SocialPlatform.TIKTOK,null,"100xtoday").getTotalElements()).isEqualTo(1);
    }
}
