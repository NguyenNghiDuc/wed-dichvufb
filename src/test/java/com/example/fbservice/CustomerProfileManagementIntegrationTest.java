package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerprofilemanagement;DB_CLOSE_DELAY=-1")
class CustomerProfileManagementIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    private void create(String username, String email) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("$2a$10$fake-password-hash-kept");
        u.setEmail(email);
        u.setRole("USER");
        users.save(u);
    }

    @Test @WithMockUser(username="profile-edit-owner")
    void editNeverModifiesAccountEmailRolePasswordAndIsOwnerScoped() throws Exception {
        create("profile-edit-owner", "owner@example.com");
        create("profile-edit-other", "other@example.com");
        mvc.perform(put("/api/v1/customer/profile").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Nguyễn Lan\",\"phone\":\"0901234567\",\"username\":\"profile-edit-other\",\"role\":\"ADMIN\",\"email\":\"evil@example.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("profile-edit-owner"))
            .andExpect(jsonPath("$.email").value("owner@example.com"))
            .andExpect(jsonPath("$.displayName").value("Nguyễn Lan"))
            .andExpect(jsonPath("$.phone").value("0901234567"))
            .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(get("/api/v1/customer/profile").with(user("profile-edit-other")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value(""))
            .andExpect(jsonPath("$.email").value("other@example.com"));
        mvc.perform(get("/profile"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Nguyễn Lan")))
            .andExpect(content().string(not(containsString("fake-password-hash-kept"))));
        User unchanged = users.findByUsername("profile-edit-owner").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("owner@example.com", unchanged.getEmail());
        org.junit.jupiter.api.Assertions.assertEquals("USER", unchanged.getRole());
        org.junit.jupiter.api.Assertions.assertEquals("$2a$10$fake-password-hash-kept", unchanged.getPassword());
    }

    @Test @WithMockUser(username="profile-invalid-owner")
    void rejectsInvalidAndMissingCsrf() throws Exception {
        create("profile-invalid-owner", "valid@example.com");
        mvc.perform(put("/api/v1/customer/profile").contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"Valid Name\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/customer/profile").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"A\",\"phone\":\"0901234567\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/customer/profile").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Valid Name\",\"phone\":\"xyz\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/profile/edit").with(csrf())
            .param("displayName", "Hồ sơ hợp lệ").param("phone", "0901234567"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile?updated=true"));
        mvc.perform(get("/profile/edit")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Hồ sơ hợp lệ")));
    }

    @Test void anonymousCannotManageProfile() throws Exception {
        mvc.perform(get("/api/v1/customer/profile")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/profile/edit")).andExpect(status().is3xxRedirection());
        mvc.perform(put("/api/v1/customer/profile").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"Test customer\"}"))
            .andExpect(status().is3xxRedirection());
    }
}
