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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:reviewSearch26;DB_CLOSE_DELAY=-1")
class CustomerReviewSearchIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;
    @Autowired CampaignReviewService reviews;

    private void reviewed(String owner, int rating, String feedback) {
        var order=campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@ownerrecon", "Hợp tác tư vấn truyền thông an toàn."));
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.APPROVED,"staff");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.IN_PROGRESS,"staff");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.COMPLETED,"staff");
        reviews.submit(owner,order.getId(),new CampaignReviewInput(rating,feedback));
    }

    @Test
    @WithMockUser(username="review-filter-owner")
    void starsAndLiteralFeedbackSearchStayInOwnerScope() throws Exception {
        reviewed("review-filter-owner",5,"Rất hài lòng với kế hoạch mới");
        reviewed("review-filter-owner",2,"Cần cải thiện 100% nội dung <script>alert(1)</script>");
        reviewed("review-filter-foreign",2,"Cần cải thiện 100% nội dung riêng của khách khác");

        mvc.perform(get("/api/v1/customer/reviews").param("rating","2"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control",containsString("no-store")))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].rating").value(2))
            .andExpect(jsonPath("$.content[0].ownerUsername").doesNotExist());
        mvc.perform(get("/api/v1/customer/reviews").param("q","%"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/customer/reviews").param("q","_"))
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/customer/reviews").param("rating","5").param("q","cải thiện"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/campaigns/reviews").param("rating","2").param("q","100%"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("100% nội dung")))
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
            .andExpect(content().string(not(containsString("riêng của khách khác"))));
        mvc.perform(get("/api/v1/customer/reviews").with(user("review-filter-outsider"))
            .param("q","100%"))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test @WithMockUser(username="review-filter-validation")
    void invalidRatingSearchAndPagingFailWithoutBroadeningQuery() throws Exception {
        mvc.perform(get("/api/v1/customer/reviews").param("rating","0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns/reviews").param("rating","6"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns/reviews").param("q","x".repeat(81)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/customer/reviews").param("size","0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/customer/reviews").param("page","-1"))
            .andExpect(status().isBadRequest());
    }

    @Test void anonymousCannotSearchCustomerReviewHistory() throws Exception {
        mvc.perform(get("/api/v1/customer/reviews").param("rating","2"))
            .andExpect(status().is3xxRedirection());
    }
}
