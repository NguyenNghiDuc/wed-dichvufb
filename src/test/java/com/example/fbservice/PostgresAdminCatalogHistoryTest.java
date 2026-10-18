package com.example.fbservice;
import com.example.fbservice.admin.AdminCatalogHistoryService;
import com.example.fbservice.campaign.CampaignServiceType;
import com.example.fbservice.catalog.ServiceCatalogAudit;
import com.example.fbservice.catalog.ServiceCatalogAuditRepository;
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
class PostgresAdminCatalogHistoryTest {
    @Autowired AdminCatalogHistoryService history;
    @Autowired ServiceCatalogAuditRepository repo;
    @Test void exactCatalogAuditSearchOnPostgres() {
        repo.save(new ServiceCatalogAudit("catalog-pg-a",CampaignServiceType.CONTENT_PLAN.name(),11000,22000,true,false));
        repo.save(new ServiceCatalogAudit("catalog-pg-b",CampaignServiceType.CONTENT_PLAN.name(),11000,33000,true,true));
        assertThat(history.page(0,CampaignServiceType.CONTENT_PLAN,"catalog-pg-a").events())
            .extracting(ServiceCatalogAudit::getNewPrice).containsExactly(22000L);
        assertThat(history.page(0,CampaignServiceType.PROFILE_AUDIT,"catalog-pg-a").events())
            .isEmpty();
    }
}
