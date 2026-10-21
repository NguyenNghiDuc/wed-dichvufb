package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.catalog.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:catalogtest;DB_CLOSE_DELAY=-1")
class ServiceCatalogIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ServiceCatalogService catalog;
    @Autowired CampaignOrderService orders;

    @Test @WithMockUser(username="catalog-admin",roles="ADMIN")
    void priceChangesApplyOnlyToNewOrdersAndInactivePackagesReject() throws Exception {
        var type=CampaignServiceType.PROFILE_AUDIT;
        var request=new CampaignOrderRequest(SocialPlatform.TIKTOK,type,
            "https://www.tiktok.com/@sample","Cần đánh giá và tối ưu trang hồ sơ để tăng chất lượng.");
        long original=catalog.quote(type);
        var first=orders.create("customer-catalog",request);
        assertThat(first.getQuotedPriceVnd()).isEqualTo(original);

        mvc.perform(get("/admin/catalog")).andExpect(status().isOk());
        mvc.perform(post("/admin/catalog/PROFILE_AUDIT").with(csrf())
                .param("price","99000").param("enabled","true"))
            .andExpect(status().is3xxRedirection());
        var second=orders.create("customer-catalog",request);
        assertThat(first.getQuotedPriceVnd()).isEqualTo(original);
        assertThat(second.getQuotedPriceVnd()).isEqualTo(99000);
        mvc.perform(get("/api/v1/services"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("99000")));
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("99.000 đ")));
        assertThat(catalog.recent()).isNotEmpty();

        mvc.perform(post("/admin/catalog/PROFILE_AUDIT").with(csrf())
                .param("price","99000").param("enabled","false"))
            .andExpect(status().is3xxRedirection());
        assertThatThrownBy(()->orders.create("customer-catalog",request))
            .isInstanceOf(IllegalArgumentException.class);
        mvc.perform(get("/api/v1/services"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("PROFILE_AUDIT"))));
        mvc.perform(post("/admin/catalog/PROFILE_AUDIT").with(user("ordinary")).with(csrf())
                .param("price","10000").param("enabled","true"))
            .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username="catalog-admin",roles="ADMIN")
    void preventClientPriceChangeAndInvalidBounds() throws Exception {
        mvc.perform(post("/admin/catalog/CONTENT_PLAN").with(csrf())
                .param("price","1").param("enabled","true"))
            .andExpect(status().isOk());
        assertThat(catalog.quote(CampaignServiceType.CONTENT_PLAN)).isEqualTo(149000);
    }
}
