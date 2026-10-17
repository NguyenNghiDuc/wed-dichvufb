package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.notification.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:notiftest;DB_CLOSE_DELAY=-1")
class NotificationsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;
    @Autowired AccountNotificationRepository notifications;

    @Test @WithMockUser(username="test_notice_owner")
    void notificationOwnershipAndAudit() throws Exception{
        var order=campaigns.create("test_notice_owner",new CampaignOrderRequest(SocialPlatform.TIKTOK,
            CampaignServiceType.CONTENT_PLAN,"https://www.tiktok.com/@valid",
            "Nội dung chia sẻ cách học tiếng Anh hàng ngày."));
        assertThat(campaigns.historyForOwner("test_notice_owner",order.getId())).hasSize(1);
        mvc.perform(get("/notifications")).andExpect(status().isOk());
        var item=notifications.findTop100ByOwnerUsernameOrderByCreatedAtDesc("test_notice_owner").get(0);
        mvc.perform(post("/notifications/"+item.getId()+"/read").with(csrf()).with(user("other_person")))
           .andExpect(status().isNotFound());
        mvc.perform(post("/notifications/"+item.getId()+"/read").with(csrf()))
           .andExpect(status().is3xxRedirection());
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("test_notice_owner")).isZero();
        mvc.perform(get("/campaigns/"+order.getId())).andExpect(status().isOk());
        mvc.perform(get("/campaigns/"+order.getId()).with(user("other_person")))
           .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(username="test_administrator",roles="ADMIN")
    void adminStateChangesNotifyOwner() throws Exception{
        var order=campaigns.create("another_campaign_owner",new CampaignOrderRequest(SocialPlatform.YOUTUBE,
            CampaignServiceType.PROFILE_AUDIT,"https://youtube.com/@validchannel",
            "Cần đánh giá và tối ưu kênh mới về công nghệ."));
        mvc.perform(post("/admin/campaign-orders/"+order.getId()+"/status").with(csrf())
                .param("status","APPROVED"))
           .andExpect(status().is3xxRedirection());
        assertThat(campaigns.historyForOwner("another_campaign_owner",order.getId())).hasSize(2);
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("another_campaign_owner")).isEqualTo(2);
    }
}
