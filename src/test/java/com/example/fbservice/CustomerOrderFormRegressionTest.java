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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerorderform;DB_CLOSE_DELAY=-1")
class CustomerOrderFormRegressionTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService orders;

    @Test @WithMockUser(username="order-form-owner")
    void invalidOrderRetainsSubmittedFieldsWithoutEscapingOrPricingBypass() throws Exception {
        String text="Mục tiêu <script>alert('x')</script> cần được kiểm duyệt.";
        mvc.perform(post("/campaigns").with(csrf())
                .param("platform","YOUTUBE").param("serviceType","CONTENT_PLAN")
                .param("targetUrl","https://bad-site.example.com")
                .param("brief",text))
            .andExpect(status().isOk())
            .andExpect(view().name("campaign_form"))
            .andExpect(model().attributeExists("error", "submittedBrief", "submittedTargetUrl"))
            .andExpect(model().attribute("submittedServiceType", CampaignServiceType.CONTENT_PLAN))
            .andExpect(content().string(containsString("https://bad-site.example.com")))
            .andExpect(content().string(not(containsString("<script>alert('x')</script>"))))
            .andExpect(content().string(containsString("&lt;script&gt;")));
        mvc.perform(post("/campaigns").param("platform","YOUTUBE")
                .param("serviceType","CONTENT_PLAN").param("targetUrl","https://youtube.com/@good")
                .param("brief","Hướng dẫn đăng bài hợp pháp"))
            .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username="order-form-cancel")
    void cancellationErrorsStayVisibleAfterRedirectAndValidCancelConfirms() throws Exception {
        CampaignOrder item=orders.create("order-form-cancel",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@good", "Chiến dịch phát triển nội dung thực tế"));
        mvc.perform(post("/campaigns/" + item.getId() + "/cancel").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns"))
            .andExpect(flash().attributeExists("notice"));
        mvc.perform(post("/campaigns/" + item.getId() + "/cancel").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attributeExists("error"));
    }
}
