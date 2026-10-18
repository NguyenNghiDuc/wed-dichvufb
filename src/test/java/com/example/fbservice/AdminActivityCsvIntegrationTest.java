package com.example.fbservice;

import com.example.fbservice.admin.*;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4activitycsv;DB_CLOSE_DELAY=-1")
class AdminActivityCsvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AdminAccountAuditRepository audits;

    @Test
    @WithMockUser(username="w4-activity-export-admin",roles="ADMIN")
    void exportsOnlyBoundedEventDataWithFormulaInjectionProtection() throws Exception {
        audits.save(new AdminAccountAudit("  =HYPERLINK(\"https://evil.invalid\",\"click\")",
            1234L, "ACCOUNT_ROLE", "USER", "STAFF"));
        for (int i=0;i<65;i++)
            audits.save(new AdminAccountAudit("w4-activity-export-admin",
                3000L+i, "ACCOUNT_ENABLED", "true", "false"));
        var response=mvc.perform(get("/admin/activity/export.csv").param("area", "ACCOUNTS"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("Content-Disposition", containsString("attachment")))
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andReturn().getResponse();
        String value=response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(value).contains("timestamp,area,actor,target,details")
            .doesNotContain("password", "secret_key", "email_address");
        assertThat(value.split("\r\n").length).isLessThanOrEqualTo(52);
        mvc.perform(get("/admin/activity/export.csv").param("area", "UNTRUSTED"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void csvAccessIsLimitedToAdmin() throws Exception {
        mvc.perform(get("/admin/activity/export.csv")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/activity/export.csv").with(user("w4-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/activity/export.csv").with(user("w4-customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
