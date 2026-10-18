package com.example.fbservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
    "spring.datasource.url=jdbc:h2:mem:w4adminboundaries;DB_CLOSE_DELAY=-1")
class AdminRouteAuthorizationRegressionTest {
    @Autowired MockMvc mvc;

    private static final String[] ADMIN_ONLY_ROUTES = {
        "/admin", "/admin/dashboard", "/admin/users", "/admin/users/audit",
        "/admin/activity", "/admin/activity/export.csv",
        "/admin/catalog", "/admin/catalog/history",
        "/admin/campaign-orders", "/admin/attention", "/admin/staff",
        "/admin/reports/overview.csv"
    };

    @Test
    void userAndStaffCannotReadAdminOnlyResources() throws Exception {
        for (String route : ADMIN_ONLY_ROUTES) {
            mvc.perform(get(route).with(user("w4-staff-read").roles("STAFF")))
                .andExpect(status().isForbidden());
            mvc.perform(get(route).with(user("w4-client-read").roles("USER")))
                .andExpect(status().isForbidden());
        }
    }

    @Test
    void anonymousCannotReadAdminOnlyResources() throws Exception {
        for (String route : ADMIN_ONLY_ROUTES)
            mvc.perform(get(route)).andExpect(status().is3xxRedirection());
    }

    @Test
    void adminStateChangesRequireCsrfEvenWithAdminRole() throws Exception {
        mvc.perform(post("/admin/users/99999/role")
                .with(user("w4-admin-role").roles("ADMIN")).param("role", "STAFF"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/users/99999/enabled")
                .with(user("w4-admin-lock").roles("ADMIN")).param("enabled", "false"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/campaign-orders/99999/status")
                .with(user("w4-admin-order").roles("ADMIN")).param("status", "APPROVED"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/campaign-orders/99999/assign")
                .with(user("w4-admin-assign").roles("ADMIN")).param("employeeId", "1"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/catalog/CONTENT_PLAN")
                .with(user("w4-admin-price").roles("ADMIN")).param("price", "20000"))
            .andExpect(status().isForbidden());
    }
}
