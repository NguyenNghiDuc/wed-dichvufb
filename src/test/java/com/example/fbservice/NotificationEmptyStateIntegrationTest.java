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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:notificationemptystates;DB_CLOSE_DELAY=-1")
class NotificationEmptyStateIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AccountNotificationRepository notifications;

    @Test
    @WithMockUser(username="empty-notifications-owner")
    void newOwnerSeesWelcomeRatherThanMisleadingFilterMessage() throws Exception {
        mvc.perform(get("/notifications"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("hasAnyNotifications",false))
            .andExpect(content().string(containsString("Hộp thư chưa có thông báo")))
            .andExpect(content().string(not(containsString("Xóa bộ lọc"))));
    }

    @Test
    @WithMockUser(username="filtered-notifications-owner")
    void filteredEmptyShowsResetOnlyWhenOwnerHasMessages() throws Exception {
        notifications.save(new AccountNotification("filtered-notifications-owner","Chiến dịch","Đã xử lý"));
        mvc.perform(get("/notifications").param("q","không-khớp"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("hasAnyNotifications",true))
            .andExpect(content().string(containsString("Xóa bộ lọc")))
            .andExpect(content().string(not(containsString("Hộp thư chưa có thông báo"))));
        mvc.perform(get("/notifications").param("q","Chiến dịch"))
            .andExpect(model().attribute("hasAnyNotifications",true));
    }

    @Test
    @WithMockUser(username="last-unread-owner")
    void readingUnreadItemPreservesFiltersAndResetsPageToZero() throws Exception {
        var item=notifications.save(new AccountNotification("last-unread-owner","Đơn hàng 50%","Đang xử lý"));
        String redirect=mvc.perform(post("/notifications/"+item.getId()+"/read").with(csrf())
            .param("page","4").param("unreadOnly","true").param("days","30").param("q","50%"))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(redirect).contains("page=0","unreadOnly=true","days=30");
        assertThat(redirect).contains("q=");
        assertThat(notifications.findById(item.getId()).orElseThrow().getReadAt()).isNotNull();
    }
}
