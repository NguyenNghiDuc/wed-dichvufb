package com.example.fbservice;

import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:campaignreviewtest;DB_CLOSE_DELAY=-1")
class CampaignReviewsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    CampaignOrder create(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@reviewexample", "Hỗ trợ xây dựng nội dung hữu ích."));
    }

    @Test @WithMockUser(username="review-owner")
    void completedOwnerCanSubmitPrivateReviewOnlyOnce() throws Exception {
        CampaignOrder order = create("review-owner");
        String url = "/api/v1/campaign-orders/" + order.getId() + "/review";
        String json = "{\"rating\":5,\"feedback\":\"Dịch vụ tư vấn rất hữu ích.\"}";

        mvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isConflict());
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.APPROVED, "staff");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.IN_PROGRESS, "staff");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.COMPLETED, "staff");

        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isForbidden());
        mvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.feedback").value("Dịch vụ tư vấn rất hữu ích."))
            .andExpect(jsonPath("$.ownerUsername").doesNotExist());
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(5))
            .andExpect(header().string("Cache-Control", containsString("no-store")));
        mvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isConflict());
        mvc.perform(get(url).with(user("review-outsider"))).andExpect(status().isNotFound());
        mvc.perform(post(url).with(csrf()).with(user("review-outsider"))
            .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isNotFound());
        mvc.perform(get("/campaigns/" + order.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Đánh giá dịch vụ")))
            .andExpect(content().string(containsString("Dịch vụ tư vấn rất hữu ích.")));
    }

    @Test @WithMockUser(username="review-validation")
    void invalidRatingAndResponseFailWith400AndWebRequiresCsrf() throws Exception {
        CampaignOrder order = create("review-validation");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.APPROVED, "staff");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.IN_PROGRESS, "staff");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.COMPLETED, "staff");
        String url = "/api/v1/campaign-orders/" + order.getId() + "/review";
        mvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":0,\"feedback\":\"Nội dung hợp lệ dài đủ chữ\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5,\"feedback\":\"ngắn\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(post("/campaigns/" + order.getId() + "/review").param("rating","4")
                .param("feedback","Phản hồi đủ dài và chính xác"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/campaigns/" + order.getId() + "/review").with(csrf())
                .param("rating","4").param("feedback","Phản hồi đủ dài và chính xác"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns/" + order.getId() + "?reviewSubmitted=true"));
        mvc.perform(get(url)).andExpect(jsonPath("$.rating").value(4));
    }

    @Test void anonymousCannotReadOrWriteReviews() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/1/review"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/api/v1/campaign-orders/1/review").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"rating\":5,\"feedback\":\"Đánh giá thử nghiệm hợp lệ\"}"))
            .andExpect(status().is3xxRedirection());
    }
}
