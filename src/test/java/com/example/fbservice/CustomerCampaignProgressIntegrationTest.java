package com.example.fbservice;

import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerprogress;DB_CLOSE_DELAY=-1")
class CustomerCampaignProgressIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService orders;

    @Test @WithMockUser(username="progress-owner")
    void progressIsAccurateAndNeverExposesOtherCustomerData() throws Exception {
        CampaignOrder mine = orders.create("progress-owner",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@public", "Tạo nội dung hữu ích cho khách hàng"));
        CampaignOrder other = orders.create("progress-other",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@private", "Thực hiện nội dung kênh hợp pháp"));
        String endpoint="/api/v1/campaign-orders/"+mine.getId()+"/progress";
        mvc.perform(get(endpoint)).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.percent").value(0))
            .andExpect(jsonPath("$.steps.length()").value(4));
        orders.updateStatusForAdmin(mine.getId(),CampaignStatus.APPROVED,"trusted-admin");
        mvc.perform(get(endpoint)).andExpect(jsonPath("$.percent").value(33));
        mvc.perform(get("/campaigns/"+mine.getId()))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Tiến độ thực hiện")));
        mvc.perform(get("/api/v1/campaign-orders/"+other.getId()+"/progress"))
            .andExpect(status().isNotFound());
        mvc.perform(get(endpoint).with(user("progress-other")))
            .andExpect(status().isNotFound());
    }

    @Test void anonymousCannotReadProgress() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/1/progress"))
            .andExpect(status().is3xxRedirection());
    }
}
