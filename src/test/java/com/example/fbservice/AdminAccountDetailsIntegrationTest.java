package com.example.fbservice;

import com.example.fbservice.admin.AdminAccountAudit;
import com.example.fbservice.admin.AdminAccountAuditRepository;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4adminaccountdetails;DB_CLOSE_DELAY=-1")
class AdminAccountDetailsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AdminAccountAuditRepository audit;

    @BeforeEach void clean() {
        audit.deleteAll();
        users.deleteAll();
    }

    private User account(String username, String role, boolean enabled) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("private-credential-hash-marker");
        user.setEmail("private-email@example.invalid");
        user.setRole(role);
        user.setEnabled(enabled);
        return users.save(user);
    }

    @Test @WithMockUser(username="w4-admin-viewer",roles="ADMIN")
    void accountDetailsShowOnlyItsOwnAuditAndNoSensitiveFields() throws Exception {
        var target = account("w4-account-target","STAFF",false);
        var other = account("w4-account-other","USER",true);
        for(int i=0;i<23;i++)
            audit.save(new AdminAccountAudit("w4-actor",target.getId(),
                "ACCOUNT_ENABLED","true","false"));
        audit.save(new AdminAccountAudit("w4-actor",other.getId(),
            "ACCOUNT_ROLE","USER","STAFF"));

        var page0 = mvc.perform(get("/admin/users/"+target.getId()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin_user_details"))
            .andExpect(model().attribute("events", hasSize(20)))
            .andExpect(content().string(containsString("w4-account-target")))
            .andReturn().getResponse().getContentAsString();
        assertThat(page0).doesNotContain(
            "w4-account-other", "private-credential-hash-marker", "private-email@example.invalid");
        mvc.perform(get("/admin/users/"+target.getId()).param("page","1"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("events",hasSize(3)));
    }

    @Test @WithMockUser(username="w4-admin-viewer",roles="ADMIN")
    void missingAndInvalidIdsPagesFailClosed() throws Exception {
        mvc.perform(get("/admin/users/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/users/99999999")).andExpect(status().isNotFound());
        mvc.perform(get("/admin/users/1").param("page","-1"))
            .andExpect(status().isNotFound()); // No account can be exposed via invalid requests.
        var account=account("w4-validation-target","USER",true);
        mvc.perform(get("/admin/users/"+account.getId()).param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/users/"+account.getId()).param("page","10001"))
            .andExpect(status().isBadRequest());
    }

    @Test void onlyAdminsMayReadAccountDetails() throws Exception {
        mvc.perform(get("/admin/users/1"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/users/1").with(user("w4-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/users/1").with(user("w4-customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
