package com.example.fbservice;

import com.example.fbservice.notification.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:notificationbulkload;DB_CLOSE_DELAY=-1")
class NotificationBulkLoadIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AccountNotificationRepository notifications;

    @Test
    @WithMockUser(username="loadowner")
    void paginatesHundredsAndBulkMarksOnlyCurrentOwner() throws Exception {
        var records=new ArrayList<AccountNotification>();
        for(int i=0;i<320;i++)
            records.add(new AccountNotification("loadowner","Chiến dịch #"+i,"Nội dung thử nghiệm"));
        for(int i=0;i<130;i++)
            records.add(new AccountNotification("different-loadowner","Hỗ trợ #"+i,"Tin riêng"));
        notifications.saveAllAndFlush(records);

        mvc.perform(get("/notifications")).andExpect(status().isOk())
            .andExpect(model().attribute("items",hasSize(20)))
            .andExpect(model().attribute("unread",320L));
        mvc.perform(get("/notifications").param("page","15"))
            .andExpect(model().attribute("items",hasSize(20)));
        mvc.perform(post("/notifications/read-all").with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("loadowner")).isZero();
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("different-loadowner"))
            .isEqualTo(130);
        mvc.perform(post("/notifications/read-all").with(csrf()))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/notifications").param("unreadOnly","true"))
            .andExpect(model().attribute("items",hasSize(0)));
    }
}
