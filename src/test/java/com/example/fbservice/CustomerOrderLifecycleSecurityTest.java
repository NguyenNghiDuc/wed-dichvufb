package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.catalog.ServiceCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerlifecycleboundary;DB_CLOSE_DELAY=-1")
class CustomerOrderLifecycleSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ServiceCatalogService catalog;
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder create(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@customer-example", "Kế hoạch truyền thông với nội dung hữu ích."));
    }

    @Test @WithMockUser(username="lifecycle-owner")
    void customerCannotSelfApproveAlterPriceOrCreateOrdersForOtherOwners() throws Exception {
        long correctPrice=catalog.quote(CampaignServiceType.CONTENT_PLAN);
        String request = """
            {"platform":"YOUTUBE","serviceType":"CONTENT_PLAN",
             "targetUrl":"https://youtube.com/@customer-example",
             "brief":"Kế hoạch nội dung hợp lệ cho cộng đồng người dùng thật.",
             "ownerUsername":"forged-victim","quotedPriceVnd":1,
             "status":"COMPLETED","assigneeUsername":"forged-admin"}
            """;

        mvc.perform(post("/api/v1/campaign-orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ownerUsername").value("lifecycle-owner"))
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$.quotedPriceVnd").value(correctPrice))
            .andExpect(jsonPath("$.assigneeUsername").isEmpty());

        mvc.perform(post("/admin/campaign-orders/1/status").with(csrf())
                .param("status", "COMPLETED"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/campaign-orders").contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/campaign-orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.replace("https://youtube.com/@customer-example", "https://evil.example.com")))
            .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username="lifecycle-owner2")
    void cancellationOnlyBeforeReviewAndNeverForForeignCampaign() throws Exception {
        CampaignOrder mine=create("lifecycle-owner2");
        CampaignOrder foreign=create("lifecycle-foreign2");
        mvc.perform(post("/api/v1/campaign-orders/" + foreign.getId() + "/cancel").with(csrf()))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/campaign-orders/" + mine.getId() + "/cancel"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/campaign-orders/" + mine.getId() + "/cancel").with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/v1/campaign-orders/" + mine.getId() + "/cancel").with(csrf()))
            .andExpect(status().isBadRequest());

        CampaignOrder approved=create("lifecycle-owner2");
        campaigns.updateStatusForAdmin(approved.getId(), CampaignStatus.APPROVED, "trusted-admin");
        mvc.perform(post("/api/v1/campaign-orders/" + approved.getId() + "/cancel").with(csrf()))
            .andExpect(status().isBadRequest());
        assertThatThrownBy(() -> campaigns.updateStatusForAdmin(approved.getId(),
            CampaignStatus.COMPLETED, "trusted-admin"))
            .isInstanceOf(IllegalArgumentException.class);
        mvc.perform(get("/api/v1/campaign-orders/" + foreign.getId()))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/campaign-orders/" + approved.getId() + "/history")
                .with(user("lifecycle-foreign2")))
            .andExpect(status().isNotFound());
    }

    @Test void anonymousMutationsRemainDenied() throws Exception {
        mvc.perform(post("/api/v1/campaign-orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/v1/campaign-orders/1")).andExpect(status().is3xxRedirection());
    }
}
