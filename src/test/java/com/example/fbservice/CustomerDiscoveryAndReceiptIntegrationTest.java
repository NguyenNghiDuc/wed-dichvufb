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
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:growlab_discovery;DB_CLOSE_DELAY=-1")
class CustomerDiscoveryAndReceiptIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ServiceCatalogService catalog;
    @Autowired CampaignOrderService campaigns;

    @Test
    void publicCatalogHasTwelveConfiguredOffersAndRelatedLinks() throws Exception {
        mvc.perform(get("/dich-vu"))
            .andExpect(status().isOk()).andExpect(view().name("service_catalog"))
            .andExpect(model().attribute("count", 12))
            .andExpect(content().string(containsString("Khám phá gói tư vấn phù hợp")))
            .andExpect(content().string(containsString("79.000 đ")))
            .andExpect(content().string(containsString("/dich-vu/tiktok/profile-audit")));

        mvc.perform(get("/dich-vu/tiktok/profile-audit"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Các gói tư vấn liên quan")))
            .andExpect(content().string(containsString("/dich-vu/tiktok/content-plan")));
    }

    @Test
    void filtersWorkForVietnameseWithoutAccentsAndRejectInvalidInputs() throws Exception {
        mvc.perform(get("/dich-vu").param("q", "ke hoach").param("platform", "TIKTOK"))
            .andExpect(status().isOk()).andExpect(model().attribute("count", 1))
            .andExpect(content().string(containsString("149.000 đ")));
        mvc.perform(get("/dich-vu").param("platform", "TIKTOK")
                .param("minPrice", "80000").param("maxPrice", "200000").param("sort", "price_desc"))
            .andExpect(status().isOk()).andExpect(model().attribute("count", 1));
        mvc.perform(get("/dich-vu").param("minPrice", "200000").param("maxPrice", "100000"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/dich-vu").param("sort", "admin"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/dich-vu").param("q", "<script>alert(1)</script>"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    @Test
    void changedCatalogPricesAreUsedAndDisabledPlansDisappear() throws Exception {
        catalog.update("operator", CampaignServiceType.PROFILE_AUDIT, 123000, true);
        catalog.update("operator", CampaignServiceType.CONTENT_PLAN, 149000, false);
        mvc.perform(get("/dich-vu").param("platform", "TIKTOK"))
            .andExpect(status().isOk()).andExpect(model().attribute("count", 2))
            .andExpect(content().string(containsString("123.000 đ")))
            .andExpect(content().string(not(containsString("/dich-vu/tiktok/content-plan"))));
    }

    @Test
    @WithMockUser(username = "receipt_owner")
    void receiptNavigationHasRealPriceCsrfAndOwnerProtection() throws Exception {
        long before = campaigns.pageMine("receipt_owner", 0, 20).getTotalElements();
        mvc.perform(post("/campaigns").param("platform", "TIKTOK")
                .param("serviceType", "CONTENT_PLAN").param("receipt", "true")
                .param("targetUrl", "https://www.tiktok.com/@realuser")
                .param("brief", "Tư vấn phát triển nội dung có mục tiêu hợp lệ."))
            .andExpect(status().isForbidden());
        var result = mvc.perform(post("/campaigns").with(csrf()).param("platform", "TIKTOK")
                .param("serviceType", "CONTENT_PLAN").param("receipt", "true")
                .param("targetUrl", "https://www.tiktok.com/@realuser")
                .param("brief", "Tư vấn phát triển nội dung có mục tiêu hợp lệ.")
                .param("quotedPriceVnd", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/campaigns/*/created")).andReturn();
        String redirect = result.getResponse().getRedirectedUrl();
        assertThat(campaigns.pageMine("receipt_owner", 0, 20).getTotalElements()).isEqualTo(before + 1);
        mvc.perform(get(redirect))
            .andExpect(status().isOk()).andExpect(view().name("campaign_created"))
            .andExpect(content().string(containsString("149.000 đ")))
            .andExpect(content().string(not(containsString("1 đ"))))
            .andExpect(content().string(containsString("Chờ duyệt")));
        mvc.perform(get(redirect).with(user("another_owner"))).andExpect(status().isNotFound());
        mvc.perform(get(redirect).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()))
            .andExpect(status().is3xxRedirection());
    }
}
