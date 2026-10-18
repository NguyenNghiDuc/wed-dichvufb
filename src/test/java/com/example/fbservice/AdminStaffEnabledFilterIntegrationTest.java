package com.example.fbservice;

import com.example.fbservice.admin.AdminStaffDirectoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4enabledfilter;DB_CLOSE_DELAY=-1")
class AdminStaffEnabledFilterIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AdminStaffDirectoryService directory;

    @Test
    @WithMockUser(roles="ADMIN")
    void enabledFilterBindsAndExecutesBoundedQuery() throws Exception {
        assertThat(directory.page(0, AdminStaffDirectoryService.RoleFilter.ALL, null, true).rows()).isEmpty();
        assertThat(directory.page(0, AdminStaffDirectoryService.RoleFilter.ALL, null, false).rows()).isEmpty();
        mvc.perform(get("/admin/staff").param("enabled", "false"))
            .andExpect(status().isOk()).andExpect(model().attributeExists("enabledFilter"));
        mvc.perform(get("/admin/staff").param("enabled", "true"))
            .andExpect(status().isOk());
        mvc.perform(get("/admin/staff").param("enabled", "invalid"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff").param("page", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff").param("page", "10001"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff").param("role", "USER"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff").param("username", "bad username"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff").param("enabled", "false")
                .param("role", "STAFF").param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("hasPrevious", true))
            .andExpect(model().attribute("enabledFilter", false));
        mvc.perform(post("/admin/staff")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/staff").with(csrf()))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void staffUserAndAnonymousCannotReadDirectory() throws Exception {
        mvc.perform(get("/admin/staff").param("enabled", "false"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/staff").with(user("w4-employee").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/staff").with(user("w4-customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
