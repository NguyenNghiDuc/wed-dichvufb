package com.example.fbservice;

import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4staffactorfilter;DB_CLOSE_DELAY=-1")
class AdminStaffPerformanceActorFilterIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignEventRepository events;

    @Test @WithMockUser(username="w4-performance-admin",roles="ADMIN")
    void exactActorFilterOnHtmlApiAndCsvIsConsistent() throws Exception {
        events.save(new CampaignEvent(2001L, CampaignStatus.APPROVED,
            CampaignStatus.IN_PROGRESS,"w4-perf-user-a","Nhân viên phụ trách cập nhật tiến độ"));
        events.save(new CampaignEvent(2002L, CampaignStatus.IN_PROGRESS,
            CampaignStatus.COMPLETED,"w4-perf-user-b","Nhân viên phụ trách cập nhật tiến độ"));
        mvc.perform(get("/admin/staff/performance").param("actor","w4-perf-user-a"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("w4-perf-user-a")))
            .andExpect(content().string(not(containsString("w4-perf-user-b"))));
        mvc.perform(get("/api/v1/admin/staff/performance").param("actor","w4-perf-user-a"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$[0].actor").value("w4-perf-user-a"))
            .andExpect(jsonPath("$[0].started").value(1));
        mvc.perform(get("/admin/staff/performance.csv").param("actor","w4-perf-user-a"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("w4-perf-user-a")))
            .andExpect(content().string(not(containsString("w4-perf-user-b"))));
        mvc.perform(get("/admin/staff/performance.csv").param("actor","=IMPORTXML()"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff/performance").param("actor","x' OR 1=1"))
            .andExpect(status().isBadRequest());
    }

    @Test void filteredEndpointStillRejectsNonAdmins() throws Exception {
        mvc.perform(get("/admin/staff/performance").param("actor","w4-perf-user-a"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/staff/performance.csv").param("actor","w4-perf-user-a")
            .with(user("worker").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/staff/performance").param("actor","w4-perf-user-a")
            .with(user("customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
