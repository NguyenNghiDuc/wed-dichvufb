package com.example.fbservice;

import com.example.fbservice.notification.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:notificationdetail;DB_CLOSE_DELAY=-1")
class NotificationDetailIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AccountNotificationRepository notifications;

    @Test
    @WithMockUser(username="detail-owner")
    void detailIsOwnerScopedAndHtmlEscaped() throws Exception {
        var mine=notifications.save(new AccountNotification("detail-owner",
            "<script>alert(1)</script>", "Tin nhắn <img src=x onerror=alert(1)>"));
        var others=notifications.save(new AccountNotification("detail-other",
            "Thông báo bí mật","Không được xem"));
        mvc.perform(get("/notifications/"+mine.getId()))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
        mvc.perform(get("/notifications/"+others.getId()))
            .andExpect(status().isNotFound());
        mvc.perform(post("/notifications/"+others.getId()+"/read").with(csrf()))
            .andExpect(status().isNotFound());
        assertThat(notifications.findById(others.getId()).orElseThrow().getReadAt()).isNull();
    }

    @Test
    @WithMockUser(username="detail-mutation-owner")
    void singleReadIsIdempotentWithFilterPreservingRedirectAndCsrf() throws Exception {
        var note=notifications.save(new AccountNotification("detail-mutation-owner","Đơn 50%",
            "Đang xử lý, mã 12"));
        String path="/notifications/"+note.getId()+"/read";
        mvc.perform(post(path).param("page","2").param("days","30")
                .param("unreadOnly","true").param("q","50%"))
            .andExpect(status().isForbidden());
        mvc.perform(post(path).with(csrf()).param("page","2").param("days","30")
                .param("unreadOnly","false").param("q","50%"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/notifications?*"));
        var time=notifications.findById(note.getId()).orElseThrow().getReadAt();
        assertThat(time).isNotNull();
        mvc.perform(post(path).with(csrf()).param("days","30")
                .param("unreadOnly","true").param("q","50%"))
            .andExpect(status().is3xxRedirection());
        assertThat(notifications.findById(note.getId()).orElseThrow().getReadAt()).isEqualTo(time);
        mvc.perform(get("/notifications/"+note.getId()).with(user("detail-other")))
            .andExpect(status().isNotFound());
    }
}
