package com.example.fbservice;

import com.example.fbservice.campaign.CampaignServiceType;
import com.example.fbservice.catalog.ServiceCatalogService;
import com.example.fbservice.catalog.ServiceCatalogAuditRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4catalogversions;DB_CLOSE_DELAY=-1")
class AdminCatalogVersionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ServiceCatalogService catalog;
    @Autowired ServiceCatalogAuditRepository audit;

    @Test
    @WithMockUser(username="w4-pricing-admin", roles="ADMIN")
    void stalePriceSubmissionCannotOverwriteNewerPriceOrAuditIt() throws Exception {
        var initial=catalog.all().stream()
            .filter(s -> s.getCode().equals("AD_STRATEGY")).findFirst().orElseThrow();
        long oldVersion=initial.getVersion();
        long baselineAudit=audit.count();
        mvc.perform(get("/admin/catalog")).andExpect(status().isOk())
            .andExpect(content().string(containsString("expectedVersion")));
        mvc.perform(post("/admin/catalog/AD_STRATEGY").with(csrf())
                .param("price","399000").param("enabled","true")
                .param("expectedVersion", Long.toString(oldVersion)))
            .andExpect(status().is3xxRedirection());
        assertThat(catalog.quote(CampaignServiceType.AD_STRATEGY)).isEqualTo(399000);
        assertThat(audit.count()).isEqualTo(baselineAudit + 1);

        mvc.perform(post("/admin/catalog/AD_STRATEGY").with(csrf())
                .param("price","299000").param("enabled","true")
                .param("expectedVersion", Long.toString(oldVersion)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Bảng giá đã thay đổi")));
        assertThat(catalog.quote(CampaignServiceType.AD_STRATEGY)).isEqualTo(399000);
        assertThat(audit.count()).isEqualTo(baselineAudit + 1);
    }

    @Test
    @WithMockUser(username="w4-pricing-admin-two",roles="ADMIN")
    void invalidPricesMissingCsrfAndStaffAreRejected() throws Exception {
        long before=audit.count();
        mvc.perform(post("/admin/catalog/AD_STRATEGY").with(csrf())
                .param("price","1").param("enabled","true")
                .param("expectedVersion","0"))
            .andExpect(status().isOk());
        mvc.perform(post("/admin/catalog/AD_STRATEGY")
                .param("price","390000").param("enabled","true"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/catalog/AD_STRATEGY").with(user("w4-pricing-staff").roles("STAFF"))
                .with(csrf()).param("price","390000").param("enabled","true"))
            .andExpect(status().isForbidden());
        assertThat(audit.count()).isEqualTo(before);
    }
}
