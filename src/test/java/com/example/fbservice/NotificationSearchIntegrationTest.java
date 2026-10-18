package com.example.fbservice;

import com.example.fbservice.notification.AccountNotification;
import com.example.fbservice.notification.AccountNotificationRepository;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:notificationsearch;DB_CLOSE_DELAY=-1")
class NotificationSearchIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AccountNotificationRepository notifications;

    @Test
    @WithMockUser(username="notification-search-alice")
    void searchesOnlyOwnerAndEscapesSqlWildcards() throws Exception {
        notifications.save(new AccountNotification("notification-search-alice","Đơn 100%","Đã hoàn thành"));
        notifications.save(new AccountNotification("notification-search-alice","A_B","Đang xử lý"));
        notifications.save(new AccountNotification("notification-search-alice","Khác","Không thuộc kết quả"));
        notifications.save(new AccountNotification("notification-search-bob","Đơn 100%","Bí mật người khác"));

        mvc.perform(get("/notifications").param("q","100%"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("items",hasSize(1)))
            .andExpect(model().attribute("q","100%"));
        mvc.perform(get("/notifications").param("q","A_B"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("items",hasSize(1)));
        mvc.perform(get("/notifications").param("q","Bí mật người khác"))
            .andExpect(model().attribute("items",hasSize(0)));
        mvc.perform(get("/notifications").param("q","100%")
                .with(user("notification-search-bob")))
            .andExpect(model().attribute("items",hasSize(1)));
    }

    @Test
    @WithMockUser(username="notification-search-limits")
    void rejectsLongControlAndPreservesFilterLinks() throws Exception {
        mvc.perform(get("/notifications").param("q","x".repeat(81)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/notifications").param("q","secret\nunsafe"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/notifications").param("q","hello")
                    .param("days","30").param("unreadOnly","true"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"q\"")))
            .andExpect(content().string(containsString("hello")));
    }
}
