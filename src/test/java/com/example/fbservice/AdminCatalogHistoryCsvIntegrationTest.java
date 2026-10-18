package com.example.fbservice;

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
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4catalogauditcsv;DB_CLOSE_DELAY=-1")
class AdminCatalogHistoryCsvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ServiceCatalogAuditRepository repo;

    @BeforeEach void clean() { repo.deleteAll(); }

    private void event(String actor, CampaignServiceType type) {
        repo.save(new ServiceCatalogAudit(actor, type.name(), 15000, 35000, true, false));
    }

    @Test @WithMockUser(username="w4-catalog-csv-admin",roles="ADMIN")
    void exportRetainsExactFiltersAndCapsRows() throws Exception {
        for (int i=0;i<25;i++) event("w4-history-bulk", CampaignServiceType.CONTENT_PLAN);
        event("w4-history-other", CampaignServiceType.CONTENT_PLAN);
        event("w4-history-bulk", CampaignServiceType.PROFILE_AUDIT);

        String first = mvc.perform(get("/admin/catalog/history/export.csv")
                .param("type","CONTENT_PLAN").param("actor","w4-history-bulk")
                .param("page","0"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("X-Content-Type-Options","nosniff"))
            .andExpect(header().string("Content-Disposition",containsString("attachment")))
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(first).contains("timestamp,actor,service_code,old_price_vnd")
            .contains("w4-history-bulk")
            .doesNotContain("w4-history-other", "PROFILE_AUDIT", "password");
        assertThat(first.split("\\r\\n").length).isEqualTo(21);

        String second = mvc.perform(get("/admin/catalog/history/export.csv")
                .param("type","CONTENT_PLAN").param("actor","w4-history-bulk")
                .param("page","1"))
            .andExpect(status().isOk()).andReturn().getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        assertThat(second.split("\\r\\n").length).isEqualTo(6);
    }

    @Test @WithMockUser(username="w4-catalog-csv-admin",roles="ADMIN")
    void rejectsMalformedQueries() throws Exception {
        mvc.perform(get("/admin/catalog/history/export.csv").param("type","SOMETHING"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/catalog/history/export.csv").param("actor","%"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/catalog/history/export.csv").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/catalog/history/export.csv").param("page","10001"))
            .andExpect(status().isBadRequest());
    }

    @Test void onlyAdminsCanDownload() throws Exception {
        mvc.perform(get("/admin/catalog/history/export.csv"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/catalog/history/export.csv")
                .with(user("w4-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/catalog/history/export.csv")
                .with(user("w4-user").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
