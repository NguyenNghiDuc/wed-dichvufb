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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:quoteComparison26;DB_CLOSE_DELAY=-1")
class CustomerQuoteComparisonIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ServiceCatalogService catalog;
    @Autowired CampaignOrderService campaigns;

    @Test @WithMockUser(username="quotes-customer")
    void comparesLiveServerPricesAndExcludesUnavailablePlans() throws Exception {
        catalog.update("staff-quote-test",CampaignServiceType.PROFILE_AUDIT,119000L,true);
        catalog.update("staff-quote-test",CampaignServiceType.AD_STRATEGY,299000L,false);

        mvc.perform(get("/api/v1/campaign-orders/quotes").param("priceVnd","1")
                .param("ownerUsername","quotes-customer-other"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].serviceType").value("PROFILE_AUDIT"))
            .andExpect(jsonPath("$[0].priceVnd").value(119000))
            .andExpect(jsonPath("$[0].currency").value("VND"))
            .andExpect(jsonPath("$[0].provisional").value(true))
            .andExpect(jsonPath("$[1].serviceType").value("CONTENT_PLAN"));
        mvc.perform(get("/api/v1/campaign-orders/quotes")
                .with(user("quotes-other-customer")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].priceVnd").value(119000));
        mvc.perform(get("/api/v1/campaign-orders/quote")
                .param("serviceType","AD_STRATEGY"))
            .andExpect(status().isBadRequest());

        var order=campaigns.create("quotes-customer",
            new CampaignOrderRequest(SocialPlatform.YOUTUBE,
                CampaignServiceType.PROFILE_AUDIT,
                "https://youtube.com/@validcreator",
                "Cần tư vấn tối ưu hồ sơ theo mục tiêu rõ ràng."));
        assertThat(order.getQuotedPriceVnd()).isEqualTo(119000L);
        assertThat(order.getStatus()).isEqualTo(CampaignStatus.PENDING_REVIEW);
        assertThat(campaigns.findMineById("quotes-customer", order.getId())
            .getQuotedPriceVnd()).isEqualTo(119000L);
        assertThat(catalog.quote(CampaignServiceType.PROFILE_AUDIT)).isEqualTo(119000L);
    }

    @Test void quoteComparisonRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/quotes"))
            .andExpect(status().is3xxRedirection());
    }
}
