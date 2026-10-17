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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:orderfilterstest;DB_CLOSE_DELAY=-1")
class CampaignOrderFilterIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder create(String owner,SocialPlatform platform,String brief){
        String target=platform==SocialPlatform.YOUTUBE
            ? "https://youtube.com/@sample"
            : "https://www.tiktok.com/@sample";
        return campaigns.create(owner,new CampaignOrderRequest(
            platform,CampaignServiceType.PROFILE_AUDIT,target,brief));
    }

    @Test @WithMockUser(username="filter-alice")
    void searchAndFiltersAlwaysScopeToAuthenticatedOwner() throws Exception {
        var mine=create("filter-alice",SocialPlatform.YOUTUBE,
            "Chiến dịch alpha%_marker nội dung hữu ích.");
        create("filter-alice",SocialPlatform.TIKTOK,"Bài chia sẻ kinh nghiệm học tập");
        create("filter-bob",SocialPlatform.YOUTUBE,"Chiến dịch alpha%_marker bí mật.");
        mvc.perform(get("/api/v1/campaign-orders/page")
            .param("q","alpha%_marker").param("platform","YOUTUBE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].ownerUsername").value("filter-alice"));
        mvc.perform(get("/api/v1/campaign-orders/page").param("q",mine.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(mine.getId()));
        mvc.perform(get("/api/v1/campaign-orders/page").param("status","COMPLETED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/campaign-orders/page").with(user("filter-bob"))
            .param("q","alpha%_marker"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].ownerUsername").value("filter-bob"));
        mvc.perform(get("/campaigns").param("platform","YOUTUBE").param("q","alpha%_marker"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tìm và lọc")));
    }

    @Test @WithMockUser(username="filter-validation")
    void rejectsExcessiveInputAndRejectsUnknownStatus() throws Exception {
        mvc.perform(get("/campaigns").param("q","x".repeat(101)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").param("q","x".repeat(101)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").param("size","51"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").param("status","NOT_VALID"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousSearchRequiresLogin() throws Exception {
        mvc.perform(get("/campaigns").param("q","secret"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/v1/campaign-orders/page").param("q","secret"))
            .andExpect(status().is3xxRedirection());
    }
}
