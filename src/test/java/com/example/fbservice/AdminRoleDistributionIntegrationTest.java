package com.example.fbservice;

import com.example.fbservice.admin.*;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4rolesummary;DB_CLOSE_DELAY=-1")
class AdminRoleDistributionIntegrationTest {
    @Autowired UserRepository users;
    @Autowired AdminDashboardService dashboard;
    @Autowired MockMvc mvc;

    private void add(String username, String role, boolean enabled) {
        User u=new User();
        u.setUsername(username);u.setPassword("test-only-placeholder");
        u.setEmail(username+"@example.com");u.setRole(role);u.setEnabled(enabled);
        users.save(u);
    }

    @Test @WithMockUser(username="w4-role-operator",roles="ADMIN")
    void dashboardGroupsAccountsByRoleAndLockedStatusWithoutExposingSecrets() throws Exception {
        add("w4-role-user-1","USER",true);
        add("w4-role-user-2","USER",false);
        add("w4-role-staff-1","STAFF",true);
        add("w4-role-admin-1","ADMIN",true);
        var rows=dashboard.userRoleBreakdown();
        var normal=rows.stream().filter(x->x.role().equals("USER")).findFirst().orElseThrow();
        assertThat(normal.total()).isEqualTo(2L);
        assertThat(normal.enabled()).isEqualTo(1L);
        assertThat(normal.locked()).isEqualTo(1L);
        assertThat(rows.stream().map(AdminUserRoleCount::role)).contains("STAFF","ADMIN");

        mvc.perform(get("/admin/dashboard")).andExpect(status().isOk())
            .andExpect(view().name("admin_dashboard"))
            .andExpect(model().attributeExists("userRoles"))
            .andExpect(content().string(containsString("Phân bổ vai trò")))
            .andExpect(content().string(not(containsString("test-only-placeholder"))));
        String report=mvc.perform(get("/admin/reports/overview.csv"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(content().string(containsString("role,total_users,enabled_users,locked_users")))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(report).contains("USER,2,1,1").doesNotContain("test-only-placeholder","@example.com");
    }

    @Test void roleBreakdownIsOnlyVisibleToAdmin() throws Exception {
        mvc.perform(get("/admin/dashboard")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/dashboard").with(user("role-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/reports/overview.csv").with(user("role-user").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
