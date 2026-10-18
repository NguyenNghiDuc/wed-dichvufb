package com.example.fbservice;

import com.example.fbservice.admin.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4activitydates;DB_CLOSE_DELAY=-1")
class AdminActivityDateRangeIntegrationTest {
    @Autowired AdminAccountAuditRepository audits;
    @Autowired AdminActivityService activities;
    @Autowired MockMvc mvc;

    private void add(String actor, Long id, LocalDateTime at) {
        AdminAccountAudit audit = new AdminAccountAudit(actor,id,
            "ACCOUNT_ROLE","USER","STAFF");
        ReflectionTestUtils.setField(audit, "createdAt", at);
        audits.save(audit);
    }

    @Test @WithMockUser(username="audit-date-admin",roles="ADMIN")
    void ownerAndDayWindowAreAppliedInDatabaseBeforeExportLimit() throws Exception {
        add("audit-date-actor",70001L,LocalDateTime.of(2026,10,17,23,59));
        add("audit-date-actor",70002L,LocalDateTime.of(2026,10,18,0,0));
        add("audit-date-actor",70003L,LocalDateTime.of(2026,10,18,23,59));
        add("audit-date-actor",70004L,LocalDateTime.of(2026,10,19,0,0));
        add("audit-other-actor",70005L,LocalDateTime.of(2026,10,18,12,0));
        LocalDate date=LocalDate.of(2026,10,18);
        assertThat(activities.recent(AdminActivityService.Area.ACCOUNTS,
            "audit-date-actor",date,date)).hasSize(2);

        mvc.perform(get("/admin/activity").param("area","ACCOUNTS")
                .param("actor","audit-date-actor")
                .param("from","2026-10-18").param("to","2026-10-18"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("from", date))
            .andExpect(content().string(containsString("Tài khoản #70002")))
            .andExpect(content().string(not(containsString("Tài khoản #70004"))));
        mvc.perform(get("/admin/activity/export.csv").param("area","ACCOUNTS")
                .param("actor","audit-date-actor")
                .param("from","2026-10-18").param("to","2026-10-18"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(content().string(containsString("70003")))
            .andExpect(content().string(not(containsString("70004"))));
    }

    @Test @WithMockUser(username="audit-date-admin2",roles="ADMIN")
    void invalidAndReversedDatesAreRejected() throws Exception {
        mvc.perform(get("/admin/activity").param("from","2026-10-19").param("to","2026-10-18"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/activity/export.csv").param("from","1999-12-31"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/activity").param("from","bad-date"))
            .andExpect(status().isBadRequest());
    }

    @Test void staffAndCustomersCannotReadFilteredAudit() throws Exception {
        mvc.perform(get("/admin/activity").param("from","2026-10-18"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/activity").param("from","2026-10-18")
                .with(user("w4-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/activity/export.csv").param("from","2026-10-18")
                .with(user("w4-customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
