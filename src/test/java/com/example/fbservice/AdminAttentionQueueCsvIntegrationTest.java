package com.example.fbservice;

import com.example.fbservice.campaign.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4queuecsv;DB_CLOSE_DELAY=-1")
class AdminAttentionQueueCsvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderRepository orders;

    private CampaignOrder pending(String owner, int hoursAgo) {
        CampaignOrder order=new CampaignOrder(owner,new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@legalchannel",
            "Nội dung phát triển cộng đồng thật, phù hợp chính sách."),98000);
        LocalDateTime stamp=LocalDateTime.now().minusHours(hoursAgo);
        ReflectionTestUtils.setField(order,"createdAt",stamp);
        ReflectionTestUtils.setField(order,"updatedAt",stamp);
        return orders.save(order);
    }

    @Test @WithMockUser(username="queue-export-admin",roles="ADMIN")
    void exportMatchesPriorityQueueWithoutCustomerPersonalContent() throws Exception {
        CampaignOrder old=pending("queue-export-old",80);
        CampaignOrder recent=pending("queue-export-recent",2);
        var result=mvc.perform(get("/admin/attention/export.csv")
                .param("queue","OVERDUE_PENDING").param("page","0"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("X-Content-Type-Options","nosniff"))
            .andExpect(header().string("Content-Disposition",containsString("attachment")))
            .andReturn().getResponse();
        String csv=new String(result.getContentAsByteArray(),StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFcampaign_id,status,service_type,created_at");
        // Match CSV row boundaries: a bare numeric ID can occur in a timestamp.
        assertThat(csv).contains("\r\n"+old.getId()+",")
            .doesNotContain("\r\n"+recent.getId()+",")
            .doesNotContain("queue-export-old","queue-export-recent")
            .doesNotContain("youtube.com","@legalchannel");
        assertThat(csv.split("\r\n")).hasSize(2);
        mvc.perform(get("/admin/attention").param("queue","OVERDUE_PENDING"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Xuất CSV trang này")));
        mvc.perform(get("/admin/attention/export.csv").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/attention/export.csv").param("queue","MALICIOUS"))
            .andExpect(status().isBadRequest());
    }

    @Test void csvIsAdminOnly() throws Exception {
        mvc.perform(get("/admin/attention/export.csv"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/attention/export.csv").with(user("staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/attention/export.csv").with(user("customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
