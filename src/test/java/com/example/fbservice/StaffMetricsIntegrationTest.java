package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:staffmetrics;DB_CLOSE_DELAY=-1")
class StaffMetricsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CampaignOrderService campaigns;
    @Autowired CampaignAssignmentService assignments;
    @Autowired CampaignOrderRepository orders;
    @Autowired CampaignPerformanceService performance;

    private User createStaff(String name,String role){
        User u=new User();u.setUsername(name);u.setPassword("fixture");
        u.setRole(role);return users.save(u);
    }

    @Test
    @WithMockUser(username="metrics-admin",roles="ADMIN")
    void countsOnlyAssignedStaffAndIgnoresClosedOverdueOrders() throws Exception {
        createStaff("metrics-admin","ADMIN");
        User staff=createStaff("metrics-staff","STAFF");
        createStaff("metrics-outsider","STAFF");

        CampaignOrder order=campaigns.create("metrics-customer",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.PROFILE_AUDIT,
            "https://youtube.com/@example",
            "Phát triển nội dung cho kênh kỹ năng học tập."));
        assignments.assign(order.getId(),staff.getId(),"metrics-admin");
        // Reload after assignment: never merge a stale fixture that could erase the assignee.
        order = orders.findById(order.getId()).orElseThrow();
        order.scheduleDeadline(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5));
        orders.saveAndFlush(order);
        assertThat(performance.metricsForStaff("metrics-staff").overdue()).isEqualTo(1);
        assertThat(performance.metricsForStaff("metrics-outsider").overdue()).isZero();
        mvc.perform(get("/staff/campaign-orders").with(user("metrics-staff").roles("STAFF")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Quá hạn")));
        mvc.perform(get("/admin/dashboard")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Chiến dịch quá hạn nội bộ")));

        campaigns.updateStatusForAdmin(order.getId(),CampaignStatus.APPROVED,"metrics-admin");
        assertThat(performance.metricsForStaff("metrics-staff").overdue()).isZero();
        assignments.progress(order.getId(),CampaignStatus.IN_PROGRESS,"metrics-staff");
        assignments.progress(order.getId(),CampaignStatus.COMPLETED,"metrics-staff");
        assertThat(performance.metricsForStaff("metrics-staff").completed()).isEqualTo(1);
        assertThat(performance.metricsForStaff("metrics-staff").overdue()).isZero();
    }
}
