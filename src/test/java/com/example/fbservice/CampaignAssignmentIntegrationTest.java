package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.model.User;
import com.example.fbservice.notification.AccountNotificationRepository;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:assignmenttest;DB_CLOSE_DELAY=-1")
class CampaignAssignmentIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CampaignOrderService campaigns;
    @Autowired CampaignAssignmentService assignments;
    @Autowired CampaignAssignmentEventRepository events;
    @Autowired AccountNotificationRepository notifications;

    private User user(String name, String role, boolean enabled) {
        User user = new User();
        user.setUsername(name);
        user.setRole(role);
        user.setEnabled(enabled);
        user.setPassword("$2a$10$dummyhashforassignmenttests");
        return users.save(user);
    }

    private CampaignOrder campaign(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.PROFILE_AUDIT,
            "https://www.youtube.com/@sample",
            "Đánh giá hồ sơ và nội dung giới thiệu phần mềm giáo dục."));
    }

    @Test
    @WithMockUser(username = "assign-admin", roles = "ADMIN")
    void assignmentIsAdminOnlyAuditedAndStaffCanProgressOnlyOwn() throws Exception {
        user("assign-admin", "ADMIN", true);
        User assigned = user("worker-assigned", "STAFF", true);
        user("worker-other", "STAFF", true);
        CampaignOrder order = campaign("assignment-customer");
        Long id = order.getId();

        mvc.perform(post("/admin/campaign-orders/" + id + "/assign")
                .param("employeeId", assigned.getId().toString()))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/campaign-orders/" + id + "/assign")
                .with(user("worker-other").roles("STAFF")).with(csrf())
                .param("employeeId", assigned.getId().toString()))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/campaign-orders/" + id + "/assign").with(csrf())
                .param("employeeId", assigned.getId().toString()))
            .andExpect(status().is3xxRedirection());

        assertThat(campaigns.findMineById("assignment-customer", id).getAssigneeUsername())
            .isEqualTo("worker-assigned");
        assertThat(events.findByCampaignIdOrderByCreatedAtAsc(id)).hasSize(1);
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("worker-assigned")).isEqualTo(1);

        mvc.perform(get("/staff/campaign-orders").with(user("worker-assigned").roles("STAFF")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("assignment-customer")));
        mvc.perform(get("/staff/campaign-orders").with(user("worker-other").roles("STAFF")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("assignment-customer"))));
        mvc.perform(post("/staff/campaign-orders/" + id + "/progress")
                .with(user("worker-assigned").roles("STAFF")).with(csrf())
                .param("status", "COMPLETED"))
            .andExpect(status().is3xxRedirection());
        assertThat(campaigns.findMineById("assignment-customer", id).getStatus())
            .isEqualTo(CampaignStatus.PENDING_REVIEW);

        campaigns.updateStatusForAdmin(id, CampaignStatus.APPROVED, "assign-admin");
        mvc.perform(post("/staff/campaign-orders/" + id + "/progress")
                .with(user("worker-other").roles("STAFF")).with(csrf())
                .param("status", "IN_PROGRESS"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/staff/campaign-orders/" + id + "/progress")
                .with(user("worker-assigned").roles("STAFF"))
                .param("status", "IN_PROGRESS"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/staff/campaign-orders/" + id + "/progress")
                .with(user("worker-assigned").roles("STAFF")).with(csrf())
                .param("status", "IN_PROGRESS"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/staff/campaign-orders/" + id + "/progress")
                .with(user("worker-assigned").roles("STAFF")).with(csrf())
                .param("status", "COMPLETED"))
            .andExpect(status().is3xxRedirection());
        assertThat(campaigns.findMineById("assignment-customer", id).getStatus())
            .isEqualTo(CampaignStatus.COMPLETED);
        assertThat(campaigns.historyForOwner("assignment-customer", id)).hasSize(4);
        assertThatThrownBy(() -> assignments.assign(id, null, "assign-admin"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @WithMockUser(username = "assign-second-admin", roles = "ADMIN")
    void cannotAssignInactiveOrOrdinaryUserAndReassignmentIsLogged() throws Exception {
        user("assign-second-admin", "ADMIN", true);
        User inactive = user("inactive-staff", "STAFF", false);
        User ordinary = user("ordinary-assignment", "USER", true);
        User staff = user("another-active-worker", "STAFF", true);
        CampaignOrder order = campaign("assignment-owner2");
        Long id = order.getId();

        assertThatThrownBy(() -> assignments.assign(id, inactive.getId(), "assign-second-admin"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> assignments.assign(id, ordinary.getId(), "assign-second-admin"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> assignments.assign(id, staff.getId(), "ordinary-assignment"))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assignments.assign(id, staff.getId(), "assign-second-admin");
        assertThatThrownBy(() -> assignments.assign(id, staff.getId(), "assign-second-admin"))
            .isInstanceOf(IllegalArgumentException.class);
        assignments.assign(id, null, "assign-second-admin");
        assertThat(events.findByCampaignIdOrderByCreatedAtAsc(id)).hasSize(2);
        assertThat(campaigns.findMineById("assignment-owner2",id).getAssigneeUsername()).isNull();
        mvc.perform(get("/admin/campaign-orders/" + id + "/assignment-history"))
            .andExpect(status().isOk());
        mvc.perform(get("/admin/campaign-orders/" + id + "/assignment-history")
                .with(user("ordinary-assignment")))
            .andExpect(status().isForbidden());
    }
}
