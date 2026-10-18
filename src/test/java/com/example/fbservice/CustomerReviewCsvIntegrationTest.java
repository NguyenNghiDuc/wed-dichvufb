package com.example.fbservice;

import com.example.fbservice.campaign.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerReviewCsv26;DB_CLOSE_DELAY=-1")
class CustomerReviewCsvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;
    @Autowired CampaignReviewService reviews;

    private void review(String owner, int stars, String message) {
        var order=campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@csvreviewer", "Hỗ trợ xây dựng nội dung truyền thông hữu ích."));
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.APPROVED,"staff");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.IN_PROGRESS,"staff");
        campaigns.updateStatusForAdmin(order.getId(), CampaignStatus.COMPLETED,"staff");
        reviews.submit(owner, order.getId(), new CampaignReviewInput(stars,message));
    }

    @Test @WithMockUser(username="review-export-owner")
    void exportIsOwnerFilteredSafeForSpreadsheetsAndMatchesDisplayedFilters() throws Exception {
        review("review-export-owner",2,"=HYPERLINK(\"https://example.invalid\",\"bad\")");
        review("review-export-owner",5,"Dịch vụ đáng tin cậy và hợp lý");
        review("review-export-foreign",2,"SECRET-OTHER-CUSTOMER FEEDBACK");

        String data=mvc.perform(get("/campaigns/reviews/export.csv")
                .param("rating","2").param("q","HYPERLINK")
                .param("ownerUsername","review-export-foreign"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"my-campaign-reviews.csv\""))
            .andExpect(header().string("Cache-Control","no-store"))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(data).startsWith("\uFEFFcampaign_id,rating,feedback,created_at\r\n");
        assertThat(data).contains("'=HYPERLINK").contains("\"2\"");
        assertThat(data).doesNotContain("SECRET-OTHER-CUSTOMER")
            .doesNotContain("Dịch vụ đáng tin cậy")
            .doesNotContain("owner_username")
            .doesNotContain("review-export-foreign");

        mvc.perform(get("/campaigns/reviews").param("rating","2"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/campaigns/reviews/export.csv")));
        mvc.perform(get("/campaigns/reviews/export.csv")
                .with(user("review-export-foreign")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("SECRET-OTHER-CUSTOMER")));
        mvc.perform(get("/campaigns/reviews/export.csv")
                .with(user("review-export-outsider")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                containsString("SECRET-OTHER-CUSTOMER"))));
    }

    @Test @WithMockUser(username="review-export-invalid")
    void invalidFiltersRejectedOnExportWithoutUnboundedRead() throws Exception {
        mvc.perform(get("/campaigns/reviews/export.csv").param("rating","0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns/reviews/export.csv").param("rating","6"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns/reviews/export.csv").param("q","x".repeat(81)))
            .andExpect(status().isBadRequest());
    }

    @Test void anonymousCannotExportPrivateReviewHistory() throws Exception {
        mvc.perform(get("/campaigns/reviews/export.csv"))
            .andExpect(status().is3xxRedirection());
    }
}
