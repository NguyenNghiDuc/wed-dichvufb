package com.example.fbservice;

import com.example.fbservice.admin.AdminStaffPerformanceService;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4staffperformance;DB_CLOSE_DELAY=-1")
class AdminStaffPerformanceIntegrationTest {
    @Autowired CampaignEventRepository events;
    @Autowired AdminStaffPerformanceService stats;
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="w4-stats-admin",roles="ADMIN")
    void onlyRecordedStaffProgressCountsAndAdminCanView() throws Exception {
        events.save(new CampaignEvent(121L,CampaignStatus.APPROVED,
            CampaignStatus.IN_PROGRESS,"staff-perf-a","Nhân viên phụ trách cập nhật tiến độ"));
        events.save(new CampaignEvent(121L,CampaignStatus.IN_PROGRESS,
            CampaignStatus.COMPLETED,"staff-perf-a","Nhân viên phụ trách cập nhật tiến độ"));
        events.save(new CampaignEvent(122L,CampaignStatus.APPROVED,
            CampaignStatus.IN_PROGRESS,"staff-perf-b","Nhân viên phụ trách cập nhật tiến độ"));
        events.save(new CampaignEvent(123L,null,CampaignStatus.PENDING_REVIEW,
            "customer-perf","Chiến dịch được tạo"));
        events.save(new CampaignEvent(124L,CampaignStatus.PENDING_REVIEW,
            CampaignStatus.APPROVED,"admin-perf","Quản trị viên cập nhật trạng thái"));
        var rows=stats.lastThirtyDays();
        assertThat(rows).extracting(AdminStaffPerformanceService.StaffPerformance::actor)
            .contains("staff-perf-a","staff-perf-b")
            .doesNotContain("customer-perf","admin-perf");
        assertThat(rows.stream().filter(x->x.actor().equals("staff-perf-a")).findFirst().orElseThrow().completed())
            .isEqualTo(1);
        assertThat(stats.forActor("staff-perf-b").started()).isEqualTo(1);
        assertThat(stats.forActor("staff-perf-no-events").completed()).isZero();
        mvc.perform(get("/admin/staff/performance")).andExpect(status().isOk())
            .andExpect(view().name("admin_staff_performance"))
            .andExpect(content().string(containsString("staff-perf-a")))
            .andExpect(content().string(not(containsString("customer-perf"))));
        mvc.perform(get("/api/v1/admin/staff/performance"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$[0].actor").value("staff-perf-a"));
    }

    @Test void staffAndUsersCannotViewGlobalPerformance() throws Exception {
        mvc.perform(get("/admin/staff/performance")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/staff/performance").with(user("a-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/staff/performance").with(user("a-user").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
