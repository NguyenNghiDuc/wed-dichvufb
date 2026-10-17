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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:rejectedcopy;DB_CLOSE_DELAY=-1")
class CustomerRejectedOrderCopyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService orders;

    private CampaignOrder create(String owner) {
        return orders.create(owner, new CampaignOrderRequest(SocialPlatform.YOUTUBE,
            CampaignServiceType.CONTENT_PLAN,"https://youtube.com/@source",
            "Kế hoạch nội dung hợp pháp cho tài khoản được cấp quyền."));
    }

    @Test @WithMockUser(username="copy-owner")
    void rejectedOrderPrefillsEditableNewRequestWithoutChangingOriginal() throws Exception {
        CampaignOrder source=create("copy-owner");
        orders.updateStatusForAdmin(source.getId(), CampaignStatus.REJECTED, "trusted-admin");
        mvc.perform(get("/campaigns/"+source.getId()+"/copy"))
            .andExpect(status().isOk())
            .andExpect(view().name("campaign_form"))
            .andExpect(model().attribute("submittedPlatform", SocialPlatform.YOUTUBE))
            .andExpect(model().attribute("submittedServiceType", CampaignServiceType.CONTENT_PLAN))
            .andExpect(model().attribute("submittedTargetUrl","https://youtube.com/@source"))
            .andExpect(content().string(containsString("yêu cầu mới")));
        mvc.perform(get("/campaigns/"+source.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Tạo yêu cầu mới từ đơn bị từ chối")));
        org.junit.jupiter.api.Assertions.assertEquals(CampaignStatus.REJECTED,
            orders.findMineById("copy-owner",source.getId()).getStatus());
        mvc.perform(get("/campaigns/"+source.getId()+"/copy").with(user("copy-outsider")))
            .andExpect(status().isNotFound());
        CampaignOrder pending=create("copy-owner");
        mvc.perform(get("/campaigns/"+pending.getId()+"/copy"))
            .andExpect(status().isConflict());
    }

    @Test void anonymousCannotAccessPreviousCustomerOrder() throws Exception {
        mvc.perform(get("/campaigns/1/copy")).andExpect(status().is3xxRedirection());
    }
}
