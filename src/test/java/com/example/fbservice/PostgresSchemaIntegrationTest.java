package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.content.*;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
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
class PostgresSchemaIntegrationTest {
    @Autowired CampaignOrderService campaigns;
    @Autowired ContentDraftService drafts;
    @Autowired UserRepository users;
    @Test
    void flywayCreatesValidatedProductionSchema() {
        User user=new User();
        user.setUsername("pg_smoke_user");
        user.setPassword("not-a-real-password");
        user.setRole("USER");
        // Write only to validate the real JDBC schema, not to exercise registration logic.
        users.save(user);
        var campaign=campaigns.create("pg_smoke_user",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.PROFILE_AUDIT,
            "https://www.youtube.com/@channel", "Review kênh giáo dục về kỹ năng số."));
        assertThat(campaign.getId()).isPositive();
        assertThat(campaigns.historyForOwner("pg_smoke_user",campaign.getId())).hasSize(1);
        var draft=drafts.save("pg_smoke_user",SocialPlatform.TIKTOK,"Mẹo học",
            "Bài chia sẻ cách tự học",null);
        assertThat(draft.getId()).isPositive();
    }
}
