package com.example.fbservice;

import com.example.fbservice.admin.AdminAccountAudit;
import com.example.fbservice.admin.AdminAccountAuditRepository;
import com.example.fbservice.admin.AdminUserService;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:w4adminusers;DB_CLOSE_DELAY=-1")
class AdminUserFilteringIntegrationTest {
    @Autowired UserRepository users;
    @Autowired AdminUserService service;
    @Autowired AdminAccountAuditRepository audits;
    @Autowired MockMvc mvc;

    private User create(String name, String role, boolean enabled) {
        User user = new User();
        user.setUsername(name);
        user.setPassword("$2a$10$example-hash-is-not-used-in-this-test");
        user.setRole(role);
        user.setEnabled(enabled);
        return users.save(user);
    }

    @Test
    @WithMockUser(username="w4-search-admin", roles="ADMIN")
    void exactFiltersPreservePagingAndAvoidWildcardMatches() throws Exception {
        create("w4-search-admin", "ADMIN", true);
        create("w4-staff-alpha", "STAFF", true);
        create("w4-staff-beta", "STAFF", false);
        create("w4-user-alpha", "USER", true);
        mvc.perform(get("/admin/users").param("username", "w4-staff-alpha")
                .param("role", "STAFF").param("enabled", "true"))
            .andExpect(status().isOk()).andExpect(view().name("admin_users"))
            .andExpect(model().attribute("users", hasSize(1)))
            .andExpect(model().attribute("totalUsers", 1L))
            .andExpect(content().string(containsString("name=\"username\"")));
        mvc.perform(get("/admin/users").param("role", "STAFF").param("enabled", "false"))
            .andExpect(status().isOk()).andExpect(model().attribute("users", hasSize(1)));
        mvc.perform(get("/admin/users").param("username", "w4-staff"))
            .andExpect(status().isOk()).andExpect(model().attribute("users", empty()));
        mvc.perform(get("/admin/users").param("role", "USER").param("enabled", "false"))
            .andExpect(status().isOk()).andExpect(model().attribute("users", empty()));
    }

    @Test
    @WithMockUser(username="w4-validation-admin", roles="ADMIN")
    void invalidFiltersAndAuditPagesAreRejected() throws Exception {
        for (String bad : new String[] {"%", "a", "a".repeat(41), "abc_%"}) {
            mvc.perform(get("/admin/users").param("username", bad))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/admin/users").param("role", "SUPERADMIN"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/users").param("enabled", "maybe"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/users").param("page", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/users/audit").param("page", "10001"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username="w4-audit-admin", roles="ADMIN")
    void auditHistoryIsPagedAndReadOnly() throws Exception {
        for (int i = 0; i < 23; i++) {
            audits.save(new AdminAccountAudit("w4-audit-admin", 1000L + i,
                    "ACCOUNT_ROLE", "USER", "STAFF"));
        }
        var pageOne = service.pageAudit(0);
        var pageTwo = service.pageAudit(1);
        assertThat(pageOne.getContent()).hasSize(20);
        assertThat(pageTwo.getContent()).hasSize(3);
        mvc.perform(get("/admin/users/audit").param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("events", hasSize(3)));
        mvc.perform(post("/admin/users/audit").with(csrf()))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void roleBoundariesAndCsrfAreEnforcedOnUserMutations() throws Exception {
        User target = create("w4-target-user", "USER", true);
        mvc.perform(get("/admin/users")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/users").with(user("w4-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/users/audit").with(user("w4-user").roles("USER")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/users/" + target.getId() + "/role")
                .with(user("w4-admin").roles("ADMIN")).param("role", "STAFF"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/users/" + target.getId() + "/enabled")
                .with(user("w4-staff").roles("STAFF")).with(csrf()).param("enabled","false"))
            .andExpect(status().isForbidden());
        assertThat(users.findById(target.getId()).orElseThrow().getRole()).isEqualTo("USER");
        assertThat(users.findById(target.getId()).orElseThrow().isEnabled()).isTrue();
    }
}
