package com.example.fbservice;
import com.example.fbservice.admin.AdminCatalogHistoryService;
import com.example.fbservice.campaign.CampaignServiceType;
import com.example.fbservice.catalog.ServiceCatalogAudit;
import com.example.fbservice.catalog.ServiceCatalogAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:cataloghistoryw4;DB_CLOSE_DELAY=-1")
class AdminCatalogHistoryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AdminCatalogHistoryService history;
    @Autowired ServiceCatalogAuditRepository repo;
    @BeforeEach void reset() { repo.deleteAll(); }
    void record(String actor,CampaignServiceType service) {
        repo.save(new ServiceCatalogAudit(actor,service.name(),20000,30000,true,false));
    }
    @Test @WithMockUser(username="history-admin",roles="ADMIN")
    void exactSearchAndBoundedPagination() throws Exception {
        record("price-a",CampaignServiceType.CONTENT_PLAN);
        record("price-b",CampaignServiceType.CONTENT_PLAN);
        record("price-a",CampaignServiceType.PROFILE_AUDIT);
        assertThat(history.page(0,CampaignServiceType.CONTENT_PLAN,"price-a").events()).hasSize(1);
        mvc.perform(get("/admin/catalog/history").param("type","CONTENT_PLAN").param("actor","price-a"))
            .andExpect(status().isOk()).andExpect(model().attribute("events",hasSize(1)))
            .andExpect(view().name("admin_catalog_history"));
        mvc.perform(get("/admin/catalog/history").param("actor","price"))
            .andExpect(status().isOk()).andExpect(model().attribute("events",empty()));
        for(int i=0;i<25;i++)record("bulk-a",CampaignServiceType.CONTENT_PLAN);
        var first=history.page(0,CampaignServiceType.CONTENT_PLAN,"bulk-a");
        var next=history.page(1,CampaignServiceType.CONTENT_PLAN,"bulk-a");
        assertThat(first.events()).hasSize(20);
        assertThat(first.hasNext()).isTrue();
        assertThat(next.events()).hasSize(5);
        assertThat(next.hasNext()).isFalse();
        assertThat(first.events()).extracting(ServiceCatalogAudit::getId)
            .doesNotContainAnyElementsOf(next.events().stream().map(ServiceCatalogAudit::getId).toList());
    }
    @Test @WithMockUser(username="history-admin",roles="ADMIN")
    void rejectsInvalidFilters() throws Exception {
        mvc.perform(get("/admin/catalog/history").param("page","-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/catalog/history").param("page","10001")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/catalog/history").param("type","INVALID")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/catalog/history").param("actor","%")).andExpect(status().isBadRequest());
    }
    @Test void forbidsOtherRoles() throws Exception {
        mvc.perform(get("/admin/catalog/history")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/catalog/history").with(user("worker").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/catalog/history").with(user("customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
