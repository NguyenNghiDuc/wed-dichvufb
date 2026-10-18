package com.example.fbservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:notificationpresentation;DB_CLOSE_DELAY=-1")
class NotificationPresentationIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username="presentation-owner")
    void inboxHasAccessibleSearchEmptyStateAndExternalStylesheet() throws Exception {
        mvc.perform(get("/notifications"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/notifications.css")))
            .andExpect(content().string(containsString("role=\"search\"")))
            .andExpect(content().string(containsString("Không có thông báo phù hợp")));
        mvc.perform(get("/notifications.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("@media (max-width: 680px)")));
    }
}
