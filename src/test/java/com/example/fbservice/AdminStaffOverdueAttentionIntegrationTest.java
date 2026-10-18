package com.example.fbservice;

import com.example.fbservice.admin.AdminStaffOverviewService;
import com.example.fbservice.campaign.*;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4staffoverdue;DB_CLOSE_DELAY=-1")
class AdminStaffOverdueAttentionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired CampaignOrderService orders;
    @Autowired CampaignAssignmentService assignments;
    @Autowired AdminStaffOverviewService service;

    private User account(String username, String role) {
        User user=new User();
        user.setUsername(username);
        user.setRole(role);
        user.setPassword("test-only-non-login-password");
        user.setEnabled(true);
        return users.save(user);
    }

    private CampaignOrder assigned(String owner, User assignee, User admin, boolean inProgress, int ageDays) {
        CampaignOrder order=orders.create(owner,new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@realchannel", "Nội dung thực tế phù hợp chính sách nền tảng."));
        assignments.assign(order.getId(),assignee.getId(),admin.getUsername());
        orders.updateStatusForAdmin(order.getId(),CampaignStatus.APPROVED,admin.getUsername());
        if (inProgress) orders.updateStatusForAdmin(order.getId(),
            CampaignStatus.IN_PROGRESS,admin.getUsername());
        // JPA @PreUpdate refreshes updatedAt during assignment/approval.
        // Set an historical instant only after all legitimate lifecycle changes.
        jdbc.update("update campaign_orders set updated_at=? where id=?",
            Timestamp.valueOf(LocalDateTime.now().minusDays(ageDays)),order.getId());
        return order;
    }

    @Test @WithMockUser(username="w4-overdue-staff",roles="STAFF")
    void onlyOwnActuallyStaleOrdersContributeToAttentionCounts() throws Exception {
        User admin=account("w4-overdue-admin","ADMIN");
        User mine=account("w4-overdue-staff","STAFF");
        User other=account("w4-overdue-other","STAFF");
        assigned("w4-alert-old-approval",mine,admin,false,5);
        assigned("w4-alert-new-approval",mine,admin,false,1);
        assigned("w4-alert-old-progress",mine,admin,true,9);
        assigned("w4-alert-new-progress",mine,admin,true,1);
        assigned("w4-alert-other-owner",other,admin,true,12);

        var mineResult=service.mine(mine.getUsername());
        assertThat(mineResult.approved()).isEqualTo(2);
        assertThat(mineResult.inProgress()).isEqualTo(2);
        assertThat(mineResult.staleApproved()).isEqualTo(1);
        assertThat(mineResult.staleInProgress()).isEqualTo(1);

        mvc.perform(get("/staff/overview.json").param("username",other.getUsername()))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.staleApproved").value(1))
            .andExpect(jsonPath("$.staleInProgress").value(1));
        mvc.perform(get("/staff/overview.json").with(user(other.getUsername()).roles("STAFF")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.staleApproved").value(0))
            .andExpect(jsonPath("$.staleInProgress").value(1));
        mvc.perform(get("/staff/overview"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("chưa triển khai trên 72 giờ")))
            .andExpect(content().string(containsString("chưa cập nhật trên 7 ngày")));
    }

    @Test void cannotReadAnotherWorkersAttentionOrWithoutStaffRole() throws Exception {
        mvc.perform(get("/staff/overview.json")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/staff/overview.json").with(user("unprivileged").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
