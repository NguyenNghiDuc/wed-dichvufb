package com.example.fbservice;

import com.example.fbservice.security.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties={
    "spring.datasource.url=jdbc:h2:mem:activityretention;DB_CLOSE_DELAY=-1",
    "growth.security.activity-cleanup-enabled=false",
    "growth.security.activity-retention-days=90"
})
class SecurityActivityRetentionIntegrationTest {
    @Autowired SecurityActivityRepository events;
    @Autowired SecurityActivityRetentionService retention;

    @Test
    void deletesOnlyExpiredEventsAndDoesNotDuplicateCleanup() {
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        var outdated=new SecurityActivity(37001L,SecurityActivity.Kind.SIGN_IN);
        var fresh=new SecurityActivity(37002L,SecurityActivity.Kind.PASSWORD_CHANGED);
        ReflectionTestUtils.setField(outdated,"createdAt",now.minusDays(91));
        ReflectionTestUtils.setField(fresh,"createdAt",now.minusDays(89));
        events.saveAndFlush(outdated);
        events.saveAndFlush(fresh);

        assertThat(retention.cleanupAt(now)).isEqualTo(1);
        assertThat(events.existsById(outdated.getId())).isFalse();
        assertThat(events.existsById(fresh.getId())).isTrue();
        assertThat(retention.cleanupAt(now)).isZero();
    }

    @Test
    void cleanupIsBoundedAtFiveHundredEvents() {
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        var batch=new java.util.ArrayList<SecurityActivity>();
        for(int i=0;i<503;i++) {
            SecurityActivity e=new SecurityActivity(37003L,SecurityActivity.Kind.SIGN_IN);
            ReflectionTestUtils.setField(e,"createdAt",now.minusDays(95));
            batch.add(e);
        }
        events.saveAllAndFlush(batch);
        assertThat(retention.cleanupAt(now)).isEqualTo(500);
        assertThat(retention.cleanupAt(now)).isEqualTo(3);
        assertThat(retention.cleanupAt(now)).isZero();
    }
}
