package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4layout;DB_CLOSE_DELAY=-1")
class AdminWorkspaceSmokeTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    @WithMockUser(username="w4-layout-admin",roles="ADMIN")
    void adminStylesAndKeyPagesRenderWithoutThirdPartyAssets() throws Exception {
        mvc.perform(get("/admin.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(".admin-body")))
            .andExpect(content().string(containsString("@media(max-width:700px)")))
            .andExpect(content().string(containsString("focus-visible")));

        for (String path : new String[] {
                "/admin/dashboard", "/admin/users", "/admin/users/audit",
                "/admin/catalog", "/admin/campaign-orders", "/admin/support"
        }) {
            mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin.css")));
        }
    }

    @Test
    @WithMockUser(username="w4-layout-staff",roles="STAFF")
    void staffSeesOnlyTheirQueueWithScopedStyles() throws Exception {
        User staff=new User();
        staff.setUsername("w4-layout-staff");
        staff.setRole("STAFF");
        staff.setPassword("$2a$10$test-only-not-a-real-login");
        users.save(staff);
        mvc.perform(get("/staff/campaign-orders"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/admin.css")));
        mvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
    }
}
