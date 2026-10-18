package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.catalog.ServiceCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "checkout_owner")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:checkoutflow;DB_CLOSE_DELAY=-1")
class CampaignCheckoutIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderRepository orders;
    @Autowired CampaignOrderService service;
    @Autowired ServiceCatalogService catalog;
    private static final String BRIEF = "Lên kế hoạch nội dung cho kênh giáo dục.";

    @Test
    void landingSelectionPrefillsCheckoutAndEscapesUserContent() throws Exception {
        mvc.perform(get("/campaigns/new").param("platform", "TIKTOK")
                .param("serviceType", "CONTENT_PLAN").param("targetUrl", "https://www.tiktok.com/@demo")
                .param("brief", "<script>alert(1)</script>"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("selectedPlatform", SocialPlatform.TIKTOK))
            .andExpect(model().attribute("selectedService", "CONTENT_PLAN"))
            .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")));
    }

    @Test
    void failedSubmissionPreservesFieldsWithoutCreatingOrder() throws Exception {
        mvc.perform(post("/campaigns").with(csrf()).param("platform", "TIKTOK")
                .param("serviceType", "CONTENT_PLAN").param("targetUrl", "https://example.com/not-tiktok")
                .param("brief", BRIEF))
            .andExpect(status().isOk()).andExpect(view().name("campaign_form"))
            .andExpect(model().attributeExists("error"))
            .andExpect(model().attribute("selectedService", "CONTENT_PLAN"))
            .andExpect(model().attribute("selectedPlatform", SocialPlatform.TIKTOK))
            .andExpect(model().attribute("brief", BRIEF))
            .andExpect(model().attribute("targetUrl", "https://example.com/not-tiktok"));
        assertThat(orders.count()).isZero();
    }

    @Test
    void creationUsesServerPriceAndProvidesReceiptLink() throws Exception {
        mvc.perform(post("/campaigns").with(csrf()).param("platform", "TIKTOK")
                .param("serviceType", "CONTENT_PLAN").param("targetUrl", "https://www.tiktok.com/@demo")
                .param("brief", BRIEF).param("quotedPriceVnd", "1"))
            .andExpect(redirectedUrl("/campaigns"))
            .andExpect(flash().attributeExists("success", "createdOrderId"));
        var order = orders.findByOwnerUsernameOrderByCreatedAtDesc("checkout_owner").get(0);
        assertThat(order.getQuotedPriceVnd()).isEqualTo(149000);
        assertThat(order.getStatus()).isEqualTo(CampaignStatus.PENDING_REVIEW);
        mvc.perform(get("/campaigns/" + order.getId())).andExpect(status().isOk())
            .andExpect(content().string(containsString("https://www.tiktok.com/@demo")));
    }

    @Test
    void cancellationIsOwnerScopedAndRecordsHistory() throws Exception {
        var order = service.create("checkout_owner", new CampaignOrderRequest(
            SocialPlatform.TIKTOK, CampaignServiceType.CONTENT_PLAN, "https://www.tiktok.com/@demo", BRIEF));
        mvc.perform(post("/campaigns/" + order.getId() + "/cancel").with(user("someone_else")).with(csrf()))
            .andExpect(status().isNotFound());
        assertThat(order.getStatus()).isEqualTo(CampaignStatus.PENDING_REVIEW);
        mvc.perform(post("/campaigns/" + order.getId() + "/cancel").with(csrf()))
            .andExpect(redirectedUrl("/campaigns")).andExpect(flash().attributeExists("success"));
        assertThat(order.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        assertThat(service.historyForOwner("checkout_owner", order.getId())).hasSize(2);
        mvc.perform(post("/campaigns/" + order.getId() + "/cancel").with(csrf()))
            .andExpect(status().isOk()).andExpect(model().attributeExists("error"));
    }

    @Test
    void serviceDisabledAfterSelectionCannotBePurchased() throws Exception {
        catalog.update("operator", CampaignServiceType.CONTENT_PLAN, 149000, false);
        mvc.perform(post("/campaigns").with(csrf()).param("platform", "TIKTOK")
                .param("serviceType", "CONTENT_PLAN").param("targetUrl", "https://www.tiktok.com/@demo")
                .param("brief", BRIEF))
            .andExpect(status().isOk()).andExpect(model().attributeExists("error"))
            .andExpect(model().attribute("brief", BRIEF));
        assertThat(orders.count()).isZero();
    }
}
