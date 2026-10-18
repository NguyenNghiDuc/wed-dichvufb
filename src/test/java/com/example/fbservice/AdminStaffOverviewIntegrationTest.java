package com.example.fbservice;

import com.example.fbservice.admin.AdminStaffOverviewService;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4staffoverview;DB_CLOSE_DELAY=-1")
class AdminStaffOverviewIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CampaignOrderService campaigns;
    @Autowired CampaignAssignmentService assignments;
    @Autowired CampaignEventRepository events;
    @Autowired AdminStaffOverviewService overview;

    private User user(String name,String role,boolean enabled) {
        User u=new User();u.setUsername(name);u.setRole(role);
        u.setEnabled(enabled);u.setPassword("test-only-password-hash");
        return users.save(u);
    }
    private CampaignOrder order(String owner) {
        return campaigns.create(owner,new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@valid", "Nội dung phù hợp chính sách nền tảng mạng."));
    }

    @Test @WithMockUser(username="w4-my-staff",roles="STAFF")
    void selfDashboardCountsOwnAssignmentsAndRecentActivityOnly() throws Exception {
        User admin=user("w4-my-admin","ADMIN",true);
        User mine=user("w4-my-staff","STAFF",true);
        User other=user("w4-my-other","STAFF",true);
        CampaignOrder owned=order("w4-staff-customer-one");
        CampaignOrder foreign=order("w4-staff-customer-two");
        assignments.assign(owned.getId(),mine.getId(),admin.getUsername());
        assignments.assign(foreign.getId(),other.getId(),admin.getUsername());
        campaigns.updateStatusForAdmin(owned.getId(),CampaignStatus.APPROVED,admin.getUsername());
        events.save(new CampaignEvent(owned.getId(),CampaignStatus.APPROVED,
            CampaignStatus.IN_PROGRESS,mine.getUsername(),"Nhân viên phụ trách cập nhật tiến độ"));
        events.save(new CampaignEvent(foreign.getId(),CampaignStatus.APPROVED,
            CampaignStatus.IN_PROGRESS,other.getUsername(),"Nhân viên phụ trách cập nhật tiến độ"));
        var result=overview.mine(mine.getUsername());
        assertThat(result.approved()).isEqualTo(1);
        assertThat(result.inProgress()).isZero();
        assertThat(result.startedLast30Days()).isEqualTo(1);
        assertThat(overview.mine(other.getUsername()).approved()).isZero();

        mvc.perform(get("/staff/overview")).andExpect(status().isOk())
            .andExpect(view().name("staff_overview"))
            .andExpect(content().string(containsString("Công việc của tôi")))
            .andExpect(content().string(not(containsString("w4-staff-customer-two"))));
        mvc.perform(get("/staff/overview.json").param("username","w4-my-other"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.approved").value(1))
            .andExpect(jsonPath("$.startedLast30Days").value(1));
        mvc.perform(get("/staff/overview.json").with(user("w4-my-other").roles("STAFF")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approved").value(0))
            .andExpect(jsonPath("$.startedLast30Days").value(1));
    }

    @Test void userAndDisabledStaffCannotViewStaffOverview() throws Exception {
        user("w4-disabled-staff","STAFF",false);
        mvc.perform(get("/staff/overview")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/staff/overview").with(user("ordinary-user").roles("USER")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/staff/overview.json").with(user("ordinary-user").roles("USER")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/staff/overview.json").with(user("w4-disabled-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
    }
}
