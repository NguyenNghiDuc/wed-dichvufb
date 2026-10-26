package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.catalog.ServiceCatalogService;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:servicedetailpages;DB_CLOSE_DELAY=-1")
class ServiceDetailPageIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ServiceCatalogService catalog;

    @Test
    void allTwelvePublicServicePagesRenderWithoutLogin() throws Exception {
        for (var platform : SocialPlatform.values()) {
            for (var type : CampaignServiceType.values()) {
                mvc.perform(get("/dich-vu/" + slug(platform.name()) + "/" + slug(type.name())))
                    .andExpect(status().isOk()).andExpect(view().name("service_detail"))
                    .andExpect(model().attribute("platformName", platform.getDisplayName()))
                    .andExpect(model().attribute("signedIn", false))
                    .andExpect(content().string(containsString("Chọn gói này")));
            }
        }
    }

    @Test
    void unknownPlatformAndPlanReturn404() throws Exception {
        mvc.perform(get("/dich-vu/unknown/content-plan")).andExpect(status().isNotFound());
        mvc.perform(get("/dich-vu/tiktok/unknown")).andExpect(status().isNotFound());
    }

    @Test
    void hiddenPlanIsNotPubliclyAvailable() throws Exception {
        catalog.update("operator", CampaignServiceType.CONTENT_PLAN, 149000, false);
        mvc.perform(get("/dich-vu/tiktok/content-plan")).andExpect(status().isNotFound());
        mvc.perform(get("/dich-vu/tiktok/profile-audit")).andExpect(status().isOk())
            .andExpect(model().attributeExists("priceRows"));
    }

    @Test
    void pageUsesConfiguredPriceAndAuthenticatedNavigation() throws Exception {
        catalog.update("operator", CampaignServiceType.CONTENT_PLAN, 123000, true);
        mvc.perform(get("/dich-vu/tiktok/content-plan").with(user("reader")))
            .andExpect(status().isOk()).andExpect(model().attribute("signedIn", true))
            .andExpect(content().string(containsString("123.000 đ")))
            .andExpect(content().string(containsString("Chiến dịch của tôi")))
            .andExpect(content().string(containsString("serviceType=CONTENT_PLAN")));
        mvc.perform(get("/campaigns")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/catalog")).andExpect(status().is3xxRedirection());
    }

    private static String slug(String name) {
        return name.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
