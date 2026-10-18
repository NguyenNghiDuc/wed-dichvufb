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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:keywordW2;DB_CLOSE_DELAY=-1")
class CustomerCampaignKeywordSearchIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService orders;

    private CampaignOrder order(String owner, String brief, String url) {
        return orders.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN, url, brief));
    }

    @Test @WithMockUser(username="keyword-owner")
    void searchCombinesLiteralTextFiltersAndOwnerScopeAcrossUiApiAndCsv() throws Exception {
        CampaignOrder literal = order("keyword-owner",
            "Xây dựng video có mục tiêu 100% thực tế",
            "https://youtube.com/@literalowner");
        CampaignOrder urlOnly = order("keyword-owner",
            "Nội dung ngắn về chăm sóc thú cưng",
            "https://youtube.com/@uniquematch");
        CampaignOrder foreign = order("keyword-other",
            "Chiến dịch có mục tiêu 100% thuộc người khác",
            "https://youtube.com/@foreign");
        order("keyword-owner", "Mục tiêu thông thường cho cửa hàng",
            "https://youtube.com/@general");

        mvc.perform(get("/api/v1/campaign-orders/page").param("q","100%"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(literal.getId()))
            .andExpect(jsonPath("$.content[0].quotedPriceVnd").value(149000));
        mvc.perform(get("/api/v1/campaign-orders/page").param("q","uniquematch"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(urlOnly.getId()));
        mvc.perform(get("/api/v1/campaign-orders/page").param("q","_"))
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/campaign-orders/page").param("q","100%")
                .param("status","CANCELLED"))
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/campaigns").param("q","100%"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("query","100%"))
            .andExpect(model().attribute("matchedOrders",1L))
            .andExpect(content().string(containsString("value=\"100%\"")))
            .andExpect(content().string(containsString("#" + literal.getId())))
            .andExpect(content().string(not(containsString("#" + foreign.getId()))));
        String csv = mvc.perform(get("/campaigns/export.csv").param("q","100%")
                .param("ownerUsername","keyword-other"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("100% thực tế")
            .doesNotContain("thuộc người khác")
            .doesNotContain("\"" + foreign.getId() + "\"");
        mvc.perform(get("/api/v1/campaign-orders/page")
                .with(user("keyword-other")).param("q","100%"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(foreign.getId()));
        mvc.perform(get("/api/v1/campaign-orders/page")
                .with(user("keyword-outsider")).param("q","100%"))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test @WithMockUser(username="keyword-invalid")
    void rejectsLongOrControlInputAndPreservesExistingDateValidation() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/page").param("q","x".repeat(81)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns").param("q","line\nnext"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns/export.csv").param("q","x".repeat(81)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/campaigns").param("from","2026-10-20").param("to","2026-10-01"))
            .andExpect(status().isBadRequest());
    }

    @Test void anonymousCannotSearchOrdersOrDownloadCsv() throws Exception {
        mvc.perform(get("/api/v1/campaign-orders/page").param("q","owner"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/campaigns/export.csv").param("q","owner"))
            .andExpect(status().is3xxRedirection());
    }
}
