package com.example.fbservice;

import com.example.fbservice.campaign.*;
import java.nio.charset.StandardCharsets;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:campaignPlatformW2;DB_CLOSE_DELAY=-1")
class CustomerCampaignPlatformFilterIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    @Test @WithMockUser(username="platform-owner")
    void platformFilterIsConsistentAcrossHtmlApiCsvAndOnlyReturnsOwnOrders() throws Exception {
        CampaignOrder tiktok = campaigns.create("platform-owner", new CampaignOrderRequest(
            SocialPlatform.TIKTOK, CampaignServiceType.CONTENT_PLAN,
            "https://www.tiktok.com/@myplatformowner",
            "Chiến dịch giáo dục và hướng dẫn sản phẩm theo tuần."));
        CampaignOrder youtube = campaigns.create("platform-owner", new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@myplatformowner",
            "Chiến dịch giáo dục và hướng dẫn sản phẩm cho YouTube."));
        CampaignOrder outsider = campaigns.create("platform-foreign", new CampaignOrderRequest(
            SocialPlatform.TIKTOK, CampaignServiceType.CONTENT_PLAN,
            "https://www.tiktok.com/@outsider",
            "Chiến dịch giáo dục bí mật của khách khác."));

        mvc.perform(get("/api/v1/campaign-orders/page")
                .param("platform","TIKTOK").param("q","giáo dục"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(tiktok.getId()))
            .andExpect(jsonPath("$.content[0].platform").value("TIKTOK"));
        mvc.perform(get("/api/v1/campaign-orders/page").param("platform","YOUTUBE"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(youtube.getId()));
        mvc.perform(get("/campaigns").param("platform","TIKTOK").param("q","giáo dục"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("matchedOrders",1L))
            .andExpect(content().string(containsString("#" + tiktok.getId())))
            .andExpect(content().string(not(containsString("#" + outsider.getId()))));
        String csv = mvc.perform(get("/campaigns/export.csv")
                .param("platform","TIKTOK").param("q","giáo dục"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("\"" + tiktok.getId() + "\"")
            .doesNotContain("\"" + youtube.getId() + "\"")
            .doesNotContain("\"" + outsider.getId() + "\"");
        mvc.perform(get("/api/v1/campaign-orders/page")
                .with(user("platform-foreign")).param("platform","TIKTOK"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(outsider.getId()));
        mvc.perform(get("/api/v1/campaign-orders/page")
                .with(user("platform-other")).param("platform","TIKTOK"))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test @WithMockUser(username="platform-invalid")
    void invalidPlatformIsRejectedBeforeQuery() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/page").param("platform","SNAPCHAT"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns").param("platform","SNAPCHAT"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns/export.csv").param("platform","SNAPCHAT"))
            .andExpect(status().isBadRequest());
    }

    @Test void anonymousCannotFilterSomeoneElsesPlatformOrders() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/page").param("platform","TIKTOK"))
            .andExpect(status().is3xxRedirection());
    }
}
