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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:campaignAntiTamperW2;DB_CLOSE_DELAY=-1")
class CustomerCampaignPriceTamperIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService service;

    @Test @WithMockUser(username="tamper-customer")
    void ignoresUntrustedPriceOwnerAndStatusWhenCreatingCampaign() throws Exception {
        String payload = """
            {"platform":"YOUTUBE","serviceType":"CONTENT_PLAN",
             "targetUrl":"https://youtube.com/@realcreator",
             "brief":"Tư vấn phát triển nội dung thực tế và hợp pháp.",
             "quotedPriceVnd":1,"priceVnd":0,"status":"COMPLETED",
             "ownerUsername":"tamper-other","assigneeUsername":"tamper-admin"}
            """;
        var result = mvc.perform(post("/api/v1/campaign-orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quotedPriceVnd").value(149000))
            .andExpect(jsonPath("$.ownerUsername").value("tamper-customer"))
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
            .andReturn();
        Long orderId = Long.valueOf(result.getResponse().getHeader("Location")
            .replaceAll("^.*/", ""));
        assertThat(service.findMineById("tamper-customer", orderId).getQuotedPriceVnd())
            .isEqualTo(149000);
        mvc.perform(get("/api/v1/campaign-orders/"+orderId)
            .with(user("tamper-other")))
            .andExpect(status().isNotFound());
        mvc.perform(post("/admin/campaign-orders/"+orderId+"/status")
            .with(user("tamper-customer")).with(csrf())
            .param("status","APPROVED"))
            .andExpect(status().isForbidden());
        assertThat(service.findMineById("tamper-customer", orderId).getStatus())
            .isEqualTo(CampaignStatus.PENDING_REVIEW);
    }

    @Test @WithMockUser(username="tamper-other-owner")
    void noCsrfAndCrossOwnerCancellationDoNotChangeOrder() throws Exception {
        var order=service.create("tamper-real-owner", new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@ownerchannel",
            "Lập kế hoạch nội dung bền vững cho cửa hàng."));
        mvc.perform(post("/api/v1/campaign-orders/"+order.getId()+"/cancel")
            .param("reason","Tôi hủy hộ")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/campaign-orders/"+order.getId()+"/cancel")
            .with(csrf()).param("reason","Tôi hủy hộ"))
            .andExpect(status().isNotFound());
        assertThat(service.findMineById("tamper-real-owner",order.getId()).getStatus())
            .isEqualTo(CampaignStatus.PENDING_REVIEW);
    }
}
