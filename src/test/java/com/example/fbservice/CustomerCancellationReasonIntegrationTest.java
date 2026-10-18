package com.example.fbservice;

import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:campaignCancelReason26;DB_CLOSE_DELAY=-1")
class CustomerCancellationReasonIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder make(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@cancellationtest",
            "Tư vấn kế hoạch nội dung để tăng chất lượng kênh."));
    }

    @Test @WithMockUser(username="cancel-reason-owner")
    void ownerCanRecordShortReasonWithoutChangingPriceOrDisclosingIt() throws Exception {
        var order = make("cancel-reason-owner");
        var foreign = make("cancel-reason-other");
        long quote = order.getQuotedPriceVnd();
        String unsafe = "<script>alert(1)</script> đổi kế hoạch";

        mvc.perform(get("/campaigns/"+order.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Lý do hủy (không bắt buộc)")));
        mvc.perform(post("/campaigns/"+order.getId()+"/cancel").param("reason",unsafe))
            .andExpect(status().isForbidden());
        mvc.perform(post("/campaigns/"+order.getId()+"/cancel")
                .with(csrf()).param("reason",unsafe))
            .andExpect(status().is3xxRedirection());
        assertThat(campaigns.findMineById("cancel-reason-owner",order.getId())
                .getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        assertThat(campaigns.findMineById("cancel-reason-owner",order.getId())
                .getQuotedPriceVnd()).isEqualTo(quote);
        mvc.perform(get("/api/v1/campaign-orders/"+order.getId()+"/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[1].description",containsString(unsafe)))
            .andExpect(jsonPath("$.content[1].actor").doesNotExist());
        mvc.perform(get("/campaigns/"+order.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
        mvc.perform(get("/api/v1/campaign-orders/"+order.getId()+"/history")
                .with(user("cancel-reason-other")))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/campaign-orders/"+foreign.getId()+"/cancel")
                .with(csrf()).param("reason","Không thể hủy hộ"))
            .andExpect(status().isNotFound());
        assertThat(campaigns.findMineById("cancel-reason-other",foreign.getId())
                .getStatus()).isEqualTo(CampaignStatus.PENDING_REVIEW);
        mvc.perform(post("/api/v1/campaign-orders/"+order.getId()+"/cancel")
                .with(csrf()).param("reason","Again"))
            .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username="cancel-reason-validation")
    void invalidReasonLeavesPendingOrderUnchangedAndDefaultRemainsCompatible() throws Exception {
        var first=make("cancel-reason-validation");
        String path="/api/v1/campaign-orders/"+first.getId()+"/cancel";
        mvc.perform(post(path).with(csrf()).param("reason","x".repeat(201)))
            .andExpect(status().isBadRequest());
        mvc.perform(post(path).with(csrf()).param("reason","first line\nsecond line"))
            .andExpect(status().isBadRequest());
        assertThat(campaigns.findMineById("cancel-reason-validation",first.getId())
            .getStatus()).isEqualTo(CampaignStatus.PENDING_REVIEW);
        mvc.perform(get("/api/v1/campaign-orders/"+first.getId()+"/history"))
            .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(post(path).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/campaign-orders/"+first.getId()+"/history"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[1].description").value("Khách hàng hủy yêu cầu"));
    }

    @Test void anonymousCannotCancelOrReadPrivateCancellationReason() throws Exception {
        mvc.perform(post("/api/v1/campaign-orders/1/cancel").with(csrf())
                .param("reason","Other person's request"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/v1/campaign-orders/1/history"))
            .andExpect(status().is3xxRedirection());
    }
}
