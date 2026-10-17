package com.example.fbservice;

import com.example.fbservice.admin.AdminStaffDirectoryService;
import com.example.fbservice.campaign.*;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4staffdirectory;DB_CLOSE_DELAY=-1")
class AdminStaffDirectoryIntegrationTest {
    @Autowired AdminStaffDirectoryService directory;
    @Autowired UserRepository users;
    @Autowired CampaignOrderRepository orders;
    @Autowired MockMvc mvc;

    @BeforeEach
    void clearDirectoryFixtures() {
        orders.deleteAll();
        users.deleteAll();
    }

    private void account(String username, String role, boolean enabled) {
        User u = new User();u.setUsername(username);
        u.setPassword("$2a$10$unused-not-a-real-credential");
        u.setRole(role);u.setEnabled(enabled);
        users.save(u);
    }

    @Test
    @WithMockUser(username="w4-directory-admin",roles="ADMIN")
    void directoryFiltersExactStaffAndCountsActiveAssignedOrdersOnly() throws Exception {
        account("w4-directory-admin","ADMIN",true);
        account("w4-worker-a","STAFF",true);
        account("w4-worker-b","STAFF",false);
        account("w4-buyer","USER",true);
        CampaignOrder active = new CampaignOrder("w4-buyer",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,"https://youtube.com/@growlab",
            "Chiến dịch tư vấn nội dung hợp lệ."),79000);
        active.changeStatus(CampaignStatus.APPROVED);
        active.assignTo("w4-worker-a");
        orders.save(active);

        var staff = directory.page(0, AdminStaffDirectoryService.RoleFilter.STAFF,"w4-worker-a");
        assertThat(staff.rows()).hasSize(1);
        assertThat(staff.rows().get(0).activeOrders()).isEqualTo(1);
        assertThat(directory.page(0, AdminStaffDirectoryService.RoleFilter.STAFF,"w4-worker-b")
            .rows().get(0).enabled()).isFalse();
        assertThat(directory.page(0, AdminStaffDirectoryService.RoleFilter.ALL,null).rows())
            .extracting(AdminStaffDirectoryService.StaffRow::username)
            .doesNotContain("w4-buyer");
        mvc.perform(get("/admin/staff").param("role","STAFF")
            .param("username","w4-worker-a"))
            .andExpect(status().isOk()).andExpect(model().attribute("staff",hasSize(1)))
            .andExpect(content().string(containsString("w4-worker-a")));
    }

    @Test
    @WithMockUser(username="w4-directory-paging-admin",roles="ADMIN")
    void listIsBoundedAndInvalidFiltersReturn400() throws Exception {
        for(int i=0;i<25;i++) account("w4-directory-paging-"+i,"STAFF",true);
        var first=directory.page(0, AdminStaffDirectoryService.RoleFilter.STAFF, null);
        var second=directory.page(1, AdminStaffDirectoryService.RoleFilter.STAFF, null);
        assertThat(first.rows()).hasSize(20);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.rows()).hasSize(5);
        assertThat(second.hasNext()).isFalse();
        mvc.perform(get("/admin/staff").param("page","-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff").param("page","10001")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff").param("role","OWNER")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff").param("username","%")).andExpect(status().isBadRequest());
    }

    @Test
    void customerAndStaffCannotAccessTheAdminDirectory() throws Exception {
        mvc.perform(get("/admin/staff")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/staff").with(user("w4-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/staff").with(user("w4-buyer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
