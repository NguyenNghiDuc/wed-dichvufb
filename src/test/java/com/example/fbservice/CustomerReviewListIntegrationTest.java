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
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerreviewlist;DB_CLOSE_DELAY=-1")
class CustomerReviewListIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService orders;
    @Autowired CampaignReviewService reviews;

    private CampaignOrder reviewed(String owner, String feedback) {
        CampaignOrder o = orders.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@verified",
            "Nội dung truyền thông hữu ích và hợp pháp."));
        orders.updateStatusForAdmin(o.getId(), CampaignStatus.APPROVED, "staff");
        orders.updateStatusForAdmin(o.getId(), CampaignStatus.IN_PROGRESS, "staff");
        orders.updateStatusForAdmin(o.getId(), CampaignStatus.COMPLETED, "staff");
        reviews.submit(owner, o.getId(), new CampaignReviewInput(5, feedback));
        return o;
    }

    @Test @WithMockUser(username="reviews-list-owner")
    void pagedReviewsCannotExposeOtherCustomers() throws Exception {
        CampaignOrder mine=reviewed("reviews-list-owner","Dịch vụ đủ tốt để giới thiệu.");
        reviewed("reviews-list-owner","Phản hồi bổ sung hợp lệ.");
        CampaignOrder foreign=reviewed("reviews-list-other","Không được lộ nội dung riêng tư.");
        mvc.perform(get("/api/v1/customer/reviews").param("size","1"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].ownerUsername").doesNotExist());
        mvc.perform(get("/api/v1/customer/reviews").with(user("reviews-list-other")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].campaignId").value(foreign.getId()));
        mvc.perform(get("/api/v1/customer/reviews").with(user("reviews-list-empty")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/campaigns/reviews"))
            .andExpect(status().isOk())
            .andExpect(view().name("customer_reviews"))
            .andExpect(content().string(containsString("Dịch vụ đủ tốt")))
            .andExpect(content().string(not(containsString("Không được lộ"))));
        mvc.perform(get("/campaigns"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Đánh giá của tôi")));
        mvc.perform(get("/campaigns/reviews").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/customer/reviews").param("size","51"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/customer/reviews").param("page","10001"))
            .andExpect(status().isBadRequest());
    }

    @Test void noAnonymousHistoryAccess() throws Exception {
        mvc.perform(get("/campaigns/reviews")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/v1/customer/reviews")).andExpect(status().is3xxRedirection());
    }
}
