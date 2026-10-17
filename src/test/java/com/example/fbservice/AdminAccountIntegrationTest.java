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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:admintest;DB_CLOSE_DELAY=-1")
class AdminAccountIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AdminUserService accounts;
    @Autowired AdminAccountAuditRepository events;

    private User persist(String username,String role) {
        User u=new User();
        u.setUsername(username);
        u.setPassword("$2a$10$notusedinthiscase");
        u.setRole(role);
        return users.save(u);
    }

    @Test
    @WithMockUser(username="admin-control",roles="ADMIN")
    void lockAndRoleChangesAreAuditedAndOnlyAdminsAllowed() throws Exception {
        persist("admin-control","ADMIN");
        User customer=persist("customer-control","USER");
        mvc.perform(get("/admin/users")).andExpect(status().isOk());
        mvc.perform(post("/admin/users/"+customer.getId()+"/role")
                .param("role","STAFF").with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo("STAFF");
        mvc.perform(post("/admin/users/"+customer.getId()+"/enabled")
                .param("enabled","false").with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(users.findById(customer.getId()).orElseThrow().isEnabled()).isFalse();
        assertThat(events.count()).isGreaterThanOrEqualTo(2);
        mvc.perform(post("/admin/users/"+customer.getId()+"/role")
                .param("role","ADMIN").with(user("customer-control")).with(csrf()))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/users/"+customer.getId()+"/enabled")
                .param("enabled","true"))
            .andExpect(status().isForbidden());
        assertThatThrownBy(()->accounts.setEnabled("admin-control",
            users.findByUsername("admin-control").orElseThrow().getId(),false))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lockedAccountCannotLoginOrUseExistingSession() throws Exception {
        persist("admin-second","ADMIN");
        User target=persist("locked-target","USER");
        users.findById(target.getId()).orElseThrow().setPassword(
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("passphrase123"));
        users.save(target);

        var login=mvc.perform(post("/login").with(csrf())
                .param("username","locked-target").param("password","passphrase123"))
            .andExpect(status().is3xxRedirection()).andReturn();
        var session=login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/campaigns").session((org.springframework.mock.web.MockHttpSession)session))
            .andExpect(status().isOk());
        mvc.perform(post("/admin/users/"+target.getId()+"/enabled")
                .with(user("admin-second").roles("ADMIN")).with(csrf())
                .param("enabled","false"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/campaigns").session((org.springframework.mock.web.MockHttpSession)session))
            .andExpect(status().isForbidden());
        mvc.perform(post("/login").with(csrf())
                .param("username","locked-target").param("password","passphrase123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }
}
