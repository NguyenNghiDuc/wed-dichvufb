package com.example.fbservice;

import com.example.fbservice.campaign.CampaignOrderService;
import com.example.fbservice.campaign.CampaignServiceType;
import com.example.fbservice.campaign.SocialPlatform;
import com.example.fbservice.catalog.ServiceCatalogService;
import java.util.Locale;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:growlabServiceDetail;DB_CLOSE_DELAY=-1")
class ServiceDetailPageIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ServiceCatalogService catalog;
    @Autowired CampaignOrderService campaigns;

    @Test
    void landingPageIsPublicAndDoesNotFabricateStatistics() throws Exception {
        mvc.perform(get("/dich-vu/tiktok/profile-audit"))
            .andExpect(status().isOk())
            .andExpect(view().name("service_detail"))
            .andExpect(model().attribute("signedIn", false))
            .andExpect(content().string(containsString("Câu hỏi thường gặp")))
            .andExpect(content().string(containsString("Chưa hiển thị đánh giá đã xác minh")))
            .andExpect(content().string(not(containsString("12,847"))))
            .andExpect(content().string(not(containsString("name=\"targetUrl\""))));
        mvc.perform(get("/style.css")).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(content().string(containsString("/dich-vu/tiktok/profile-audit")));
    }

    @Test
    void supportedPlatformsAndPlansRenderAndInvalidPathsReturn404() throws Exception {
        for (SocialPlatform platform : SocialPlatform.values()) {
            for (CampaignServiceType type : CampaignServiceType.values()) {
                String path = "/dich-vu/" + slug(platform.name()) + "/" + slug(type.name());
                mvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("serviceTitle", containsString(platform.getDisplayName())));
            }
        }
        mvc.perform(get("/dich-vu/invalid/profile-audit")).andExpect(status().isNotFound());
        mvc.perform(get("/dich-vu/tiktok/unknown")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "service-detail-owner")
    void requestFormRequiresCsrfAndServerPriceIsAuthoritative() throws Exception {
        mvc.perform(get("/dich-vu/tiktok/profile-audit"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("signedIn", true))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(containsString("name=\"targetUrl\"")));

        mvc.perform(post("/campaigns")
                .param("platform", "TIKTOK").param("serviceType", "PROFILE_AUDIT")
                .param("targetUrl", "https://www.tiktok.com/@actualbusiness")
                .param("brief", "Cần đánh giá hồ sơ và cải thiện kế hoạch nội dung theo mục tiêu.")
                .param("price", "1")).andExpect(status().isForbidden());

        mvc.perform(post("/campaigns").with(csrf())
                .param("platform", "TIKTOK").param("serviceType", "PROFILE_AUDIT")
                .param("targetUrl", "https://www.tiktok.com/@actualbusiness")
                .param("brief", "Cần đánh giá hồ sơ và cải thiện kế hoạch nội dung theo mục tiêu.")
                .param("price", "1"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/campaigns"));

        var mine = campaigns.pageMine("service-detail-owner", 0, 20, null, null);
        assertThat(mine.getTotalElements()).isEqualTo(1);
        assertThat(mine.getContent().get(0).getQuotedPriceVnd())
                .isEqualTo(catalog.quote(CampaignServiceType.PROFILE_AUDIT));
        assertThat(campaigns.pageMine("unrelated-owner", 0, 20, null, null).getTotalElements()).isZero();
    }

    @Test
    void detailedMarketingLayoutShowsOnlyRealPlansAndValidHelpLinks() throws Exception {
        mvc.perform(get("/dich-vu/tiktok/profile-audit"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("priceRows", org.hamcrest.Matchers.hasSize(3)))
            .andExpect(content().string(containsString("Danh mục nền tảng")))
            .andExpect(content().string(containsString("GROWLAB SOCIAL STUDIO")))
            .andExpect(content().string(containsString("QUY TRÌNH 4 BƯỚC")))
            .andExpect(content().string(containsString("So sánh các gói tư vấn")))
            .andExpect(content().string(containsString("Câu hỏi thường gặp")))
            .andExpect(content().string(containsString("Đánh giá dịch vụ")))
            .andExpect(content().string(not(containsString("50,000+"))))
            .andExpect(content().string(not(containsString("Boost Credit"))));
        mvc.perform(get("/style.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(".grow-first-screen")))
            .andExpect(content().string(containsString("@media(max-width:550px)")));
    }

    private String slug(String code) {
        return code.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
