package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:roleaccessmatrix;DB_CLOSE_DELAY=-1")
class RoleAccessMatrixIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    private User create(String username, String role) {
        User u = new User();
        u.setUsername(username);
        u.setRole(role);
        u.setPassword(passwords.encode("secure-password-for-role-tests"));
        return users.saveAndFlush(u);
    }

    private MockHttpSession login(String username) throws Exception {
        var result = mvc.perform(post("/login").with(csrf())
                .param("username", username)
                .param("password", "secure-password-for-role-tests"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns"))
            .andReturn();
        var session=(MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    @Test
    void actualAuthenticatedRolesRespectAdminAndStaffBoundaries() throws Exception {
        create("matrix-admin","ADMIN");
        create("matrix-staff","STAFF");
        create("matrix-user","USER");
        var admin=login("matrix-admin");
        var staff=login("matrix-staff");
        var customer=login("matrix-user");

        mvc.perform(get("/admin/dashboard").session(admin)).andExpect(status().isOk());
        mvc.perform(get("/staff/campaign-orders").session(admin)).andExpect(status().isOk());
        mvc.perform(get("/account/password").session(admin)).andExpect(status().isOk());

        mvc.perform(get("/admin/dashboard").session(staff)).andExpect(status().isForbidden());
        mvc.perform(get("/staff/campaign-orders").session(staff)).andExpect(status().isOk());
        mvc.perform(get("/account/password").session(staff)).andExpect(status().isOk());

        mvc.perform(get("/admin/dashboard").session(customer)).andExpect(status().isForbidden());
        mvc.perform(get("/staff/campaign-orders").session(customer)).andExpect(status().isForbidden());
        mvc.perform(get("/account/password").session(customer)).andExpect(status().isOk());
    }

    @Test
    void demotedAdminCannotReuseOldSessionForReadsOrWrites() throws Exception {
        User admin=create("matrix-demoted","ADMIN");
        MockHttpSession previous=login("matrix-demoted");
        mvc.perform(get("/admin/dashboard").session(previous)).andExpect(status().isOk());

        admin.setRole("USER");
        users.saveAndFlush(admin);
        mvc.perform(get("/admin/dashboard").session(previous))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login?sessionExpired"));
        // Old principals cannot keep writing even if they have a valid CSRF token.
        var second=login("matrix-demoted");
        mvc.perform(post("/admin/catalog").session(second).with(csrf()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/campaigns").session(second)).andExpect(status().isOk());
    }
}
