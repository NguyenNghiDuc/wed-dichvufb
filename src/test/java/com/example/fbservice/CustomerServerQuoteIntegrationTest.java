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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerserverquote;DB_CLOSE_DELAY=-1")
class CustomerServerQuoteIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ServiceCatalogService catalog;

    @Test @WithMockUser(username="quote-customer")
    void quoteReflectsServerPriceAndCanNeverOverrideAmount() throws Exception {
        long current=catalog.quote(CampaignServiceType.AD_STRATEGY);
        mvc.perform(get("/api/v1/campaign-orders/quote").param("serviceType","AD_STRATEGY")
                .param("priceVnd", "1"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.priceVnd").value(current))
            .andExpect(jsonPath("$.provisional").value(true))
            .andExpect(jsonPath("$.currency").value("VND"));
        long adjusted=current+15000;
        catalog.update("trusted-admin", CampaignServiceType.AD_STRATEGY, adjusted, true);
        mvc.perform(get("/api/v1/campaign-orders/quote").param("serviceType", "AD_STRATEGY"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.priceVnd").value(adjusted));
        mvc.perform(post("/api/v1/campaign-orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                 {"platform":"YOUTUBE","serviceType":"AD_STRATEGY","quotedPriceVnd":1,
                  "targetUrl":"https://youtube.com/@growlab",
                  "brief":"Kế hoạch quảng cáo phù hợp cho cộng đồng thật"}
                 """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quotedPriceVnd").value(adjusted));
        catalog.update("trusted-admin", CampaignServiceType.AD_STRATEGY, adjusted, false);
        mvc.perform(get("/api/v1/campaign-orders/quote").param("serviceType", "AD_STRATEGY"))
            .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username="quote-validation")
    void invalidOrMissingServiceReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/quote"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/quote").param("serviceType", "UNKNOWN"))
            .andExpect(status().isBadRequest());
    }

    @Test void anonymousCannotRequestQuote() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/quote").param("serviceType", "CONTENT_PLAN"))
            .andExpect(status().is3xxRedirection());
    }
}
