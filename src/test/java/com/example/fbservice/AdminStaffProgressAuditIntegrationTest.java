package com.example.fbservice;

import com.example.fbservice.admin.*;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4staffprogressaudit;DB_CLOSE_DELAY=-1")
class AdminStaffProgressAuditIntegrationTest {
    @Autowired CampaignEventRepository events;
    @Autowired AdminActivityService activity;
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="w4-progress-admin",roles="ADMIN")
    void staffStatusIsIncludedAndDistinguishedFromAdminAndCustomerEvents() throws Exception {
        events.save(new CampaignEvent(50001L, CampaignStatus.APPROVED,
            CampaignStatus.IN_PROGRESS, "w4-progress-staff",
            "Nhân viên phụ trách cập nhật tiến độ"));
        events.save(new CampaignEvent(50002L, CampaignStatus.IN_PROGRESS,
            CampaignStatus.COMPLETED, "w4-progress-admin",
            "Quản trị viên cập nhật trạng thái"));
        events.save(new CampaignEvent(50003L, null,
            CampaignStatus.PENDING_REVIEW, "w4-progress-customer",
            "Chiến dịch được tạo"));

        assertThat(activity.recent(AdminActivityService.Area.STAFF_STATUS))
            .hasSize(1)
            .allMatch(e -> e.actor().equals("w4-progress-staff"));
        assertThat(activity.recent(AdminActivityService.Area.ORDER_STATUS))
            .hasSize(1)
            .allMatch(e -> e.actor().equals("w4-progress-admin"));
        assertThat(activity.recent(AdminActivityService.Area.ALL))
            .anyMatch(e -> e.area() == AdminActivityService.Area.STAFF_STATUS)
            .noneMatch(e -> e.actor().equals("w4-progress-customer"));
        assertThat(activity.recent(AdminActivityService.Area.STAFF_STATUS,"w4-progress-admin"))
            .isEmpty();

        mvc.perform(get("/admin/activity").param("area","STAFF_STATUS"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("w4-progress-staff")))
            .andExpect(content().string(not(containsString("w4-progress-admin"))));
        mvc.perform(get("/admin/activity/export.csv").param("area","STAFF_STATUS")
                .param("actor","w4-progress-staff"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(content().string(containsString("STAFF_STATUS")))
            .andExpect(content().string(containsString("w4-progress-staff")))
            .andExpect(content().string(not(containsString("w4-progress-customer"))));
    }

    @Test void staffAuditIsNeverExposedToStaffOrCustomers() throws Exception {
        mvc.perform(get("/admin/activity").param("area","STAFF_STATUS"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/activity").param("area","STAFF_STATUS")
                .with(user("staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/activity/export.csv").param("area","STAFF_STATUS")
                .with(user("customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
