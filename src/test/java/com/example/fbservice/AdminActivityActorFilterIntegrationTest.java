package com.example.fbservice;

import com.example.fbservice.admin.*;
import com.example.fbservice.catalog.*;
import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4auditactorfilter;DB_CLOSE_DELAY=-1")
class AdminActivityActorFilterIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AdminActivityService service;
    @Autowired AdminAccountAuditRepository accountAudits;
    @Autowired ServiceCatalogAuditRepository catalogAudits;
    @Autowired CampaignAssignmentEventRepository assignments;

    @Test @WithMockUser(username="w4-log-admin",roles="ADMIN")
    void exactActorFilterAppliesBeforeLimitAndToCsv() throws Exception {
        for (int i=0;i<55;i++)
            accountAudits.save(new AdminAccountAudit("w4-log-another",10000L+i,
                "ACCOUNT_ROLE","USER","STAFF"));
        accountAudits.save(new AdminAccountAudit("w4-log-target",4500L,
            "ACCOUNT_ENABLED","true","false"));
        catalogAudits.save(new ServiceCatalogAudit("w4-log-target","CONTENT_PLAN",
            15000L,17500L,true,true));
        assignments.save(new CampaignAssignmentEvent(12345L,null,
            "w4-assigned-staff","w4-log-another"));

        assertThat(service.recent(AdminActivityService.Area.ALL,"w4-log-target"))
            .hasSize(2)
            .allMatch(e -> e.actor().equals("w4-log-target"));
        assertThat(service.recent(AdminActivityService.Area.ACCOUNTS,"w4-log-target"))
            .hasSize(1);

        mvc.perform(get("/admin/activity").param("actor","w4-log-target"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("actorFilter","w4-log-target"))
            .andExpect(content().string(containsString("Tài khoản #4500")))
            .andExpect(content().string(not(containsString("w4-log-another"))));
        mvc.perform(get("/admin/activity/export.csv").param("actor","w4-log-target"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(content().string(containsString("w4-log-target")))
            .andExpect(content().string(not(containsString("w4-log-another"))));
        mvc.perform(get("/admin/activity").param("actor","x' OR 1=1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/activity/export.csv").param("actor","=FORMULA()"))
            .andExpect(status().isBadRequest());
    }

    @Test void actorFilteredAuditRemainsAdminOnly() throws Exception {
        mvc.perform(get("/admin/activity").param("actor","w4-log-target"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/activity").param("actor","w4-log-target")
                .with(user("w4-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/activity/export.csv").param("actor","w4-log-target")
                .with(user("w4-customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
