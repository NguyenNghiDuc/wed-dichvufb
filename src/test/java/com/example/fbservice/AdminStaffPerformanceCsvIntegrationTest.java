package com.example.fbservice;

import com.example.fbservice.campaign.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4staffperfcsv;DB_CLOSE_DELAY=-1")
class AdminStaffPerformanceCsvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignEventRepository events;

    @Test @WithMockUser(username="w4-csv-admin",roles="ADMIN")
    void exportEscapesFormulaActorAndExcludesNonStaffEventSources() throws Exception {
        events.save(new CampaignEvent(50001L,CampaignStatus.APPROVED,CampaignStatus.IN_PROGRESS,
            " =WEBSERVICE(\"https://example.invalid\")",
            "Nhân viên phụ trách cập nhật tiến độ"));
        events.save(new CampaignEvent(50002L,CampaignStatus.IN_PROGRESS,CampaignStatus.COMPLETED,
            "w4-good-staff","Nhân viên phụ trách cập nhật tiến độ"));
        events.save(new CampaignEvent(50003L,CampaignStatus.PENDING_REVIEW,CampaignStatus.APPROVED,
            "w4-admin-change","Quản trị viên cập nhật trạng thái"));
        var result=mvc.perform(get("/admin/staff/performance.csv"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("Content-Disposition",containsString("attachment")))
            .andReturn().getResponse();
        String csv=new String(result.getContentAsByteArray(),StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFactor,started_orders,completed_orders");
        assertThat(csv).contains("\"' =WEBSERVICE(\"\"https://example.invalid\"\")\"\"")
            .contains("w4-good-staff")
            .doesNotContain("w4-admin-change");
        mvc.perform(get("/admin/staff/performance")).andExpect(status().isOk())
            .andExpect(content().string(containsString("performance.csv")));
    }

    @Test void reportRequiresAdmin() throws Exception {
        mvc.perform(get("/admin/staff/performance.csv"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/staff/performance.csv").with(user("worker").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/staff/performance.csv").with(user("customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
