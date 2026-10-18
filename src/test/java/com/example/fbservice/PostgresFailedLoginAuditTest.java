package com.example.fbservice;

import com.example.fbservice.security.SecurityActivity;
import com.example.fbservice.security.SecurityActivityRepository;
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
class PostgresFailedLoginAuditTest {
    @Autowired SecurityActivityRepository events;

    @Test
    void productionSchemaStoresOnlyAccountIdKindAndUtcTime() {
        long accountId=9200141L;
        var created=events.saveAndFlush(
            new SecurityActivity(accountId, SecurityActivity.Kind.SIGN_IN_FAILED));
        assertThat(events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(accountId))
            .extracting(SecurityActivity::getKind))
            .contains(SecurityActivity.Kind.SIGN_IN_FAILED);
        assertThat(created.getCreatedAt()).isNotNull();
    }
}
