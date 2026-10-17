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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:profiletest;DB_CLOSE_DELAY=-1")
class ProfileHelpIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    void helpIsPublicButProfileRequiresLogin() throws Exception {
        mvc.perform(get("/help")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Câu hỏi thường gặp")));
        mvc.perform(get("/profile")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "profile-owner")
    void accountProfileDoesNotLeakPassword() throws Exception {
        User user = new User();
        user.setUsername("profile-owner");
        user.setPassword("$2a$10$dontdisplaypasswordhash");
        user.setEmail("sample@example.com");
        user.setRole("USER");
        users.save(user);
        mvc.perform(get("/profile")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("sample@example.com")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("$2a$10$dontdisplaypasswordhash"))));
    }
}
