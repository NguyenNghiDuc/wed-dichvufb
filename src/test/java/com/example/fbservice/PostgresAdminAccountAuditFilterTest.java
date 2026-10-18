package com.example.fbservice;

import com.example.fbservice.admin.AdminAccountAudit;
import com.example.fbservice.admin.AdminAccountAuditRepository;
import com.example.fbservice.admin.AdminUserService;
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
class PostgresAdminAccountAuditFilterTest {
    @Autowired AdminUserService service;
    @Autowired AdminAccountAuditRepository audit;

    @Test void exactActorTargetAndOperationUseRealPostgres() {
        audit.save(new AdminAccountAudit("pg-account-admin-a",410001L,
            "ACCOUNT_ENABLED","true","false"));
        audit.save(new AdminAccountAudit("pg-account-admin-b",410001L,
            "ACCOUNT_ENABLED","true","false"));
        audit.save(new AdminAccountAudit("pg-account-admin-a",410002L,
            "ACCOUNT_ROLE","USER","STAFF"));
        var results=service.pageAudit(0,"pg-account-admin-a",410001L,"ACCOUNT_ENABLED");
        assertThat(results.getTotalElements()).isEqualTo(1);
        assertThat(results.getContent()).extracting(AdminAccountAudit::getActor)
            .containsExactly("pg-account-admin-a");
        assertThat(service.pageAudit(0,"pg-account-admin-a",410001L,
            "ACCOUNT_ROLE").getTotalElements()).isZero();
        assertThat(service.pageAudit(0,"pg-account-admin",null,
            null).getTotalElements()).isZero();
    }
}
