package com.example.fbservice;

import com.example.fbservice.campaign.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerhistorycsv;DB_CLOSE_DELAY=-1")
class CustomerHistoryCsvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;

    private CampaignOrder make(String owner) {
        return campaigns.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@growlab", "Lên nội dung hữu ích cho kênh thật."));
    }

    @Test @WithMockUser(username="history-csv-owner")
    void customerMayExportOnlyTheirOwnChronologicalHistory() throws Exception {
        CampaignOrder mine=make("history-csv-owner");
        CampaignOrder foreign=make("history-csv-foreign");
        campaigns.updateStatusForAdmin(mine.getId(), CampaignStatus.APPROVED, "internal-admin");
        campaigns.updateStatusForAdmin(mine.getId(), CampaignStatus.IN_PROGRESS, "internal-admin");
        String url="/campaigns/" + mine.getId() + "/history.csv";
        mvc.perform(get(url))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Content-Disposition", containsString("attachment")))
            .andExpect(content().string(containsString("previous_status,new_status,description")))
            .andExpect(content().string(containsString("PENDING_REVIEW")))
            .andExpect(content().string(containsString("IN_PROGRESS")))
            .andExpect(content().string(not(containsString("internal-admin"))))
            .andExpect(content().string(not(containsString("history-csv-foreign"))));
        mvc.perform(get(url).with(user("history-csv-foreign")))
            .andExpect(status().isNotFound());
        mvc.perform(get("/campaigns/" + foreign.getId() + "/history.csv"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/campaigns/" + mine.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Tải lịch sử xử lý CSV")));
    }

    @Test void anonymousRequestRedirectsToLogin() throws Exception {
        mvc.perform(get("/campaigns/1/history.csv")).andExpect(status().is3xxRedirection());
    }
}
