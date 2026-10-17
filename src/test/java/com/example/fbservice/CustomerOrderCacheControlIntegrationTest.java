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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerordercache;DB_CLOSE_DELAY=-1")
class CustomerOrderCacheControlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService orders;

    @Test @WithMockUser(username="cache-owner")
    void allPersonalOrderApiResponsesPreventStaleCaching() throws Exception {
        CampaignOrder own=orders.create("cache-owner",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@channel","Kế hoạch đăng nội dung thật cho khách hàng"));
        String base="/api/v1/campaign-orders";
        mvc.perform(get(base)).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(base+"/summary")).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(base+"/page")).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(base+"/"+own.getId())).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(base+"/"+own.getId()+"/history")).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(post(base+"/"+own.getId()+"/cancel").with(csrf()))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get(base+"/"+own.getId()).with(user("cache-other")))
            .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(username="cache-create")
    void createdOrderDoesNotAllowCachedPrivateBody() throws Exception {
        mvc.perform(post("/api/v1/campaign-orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                   {"platform":"YOUTUBE","serviceType":"CONTENT_PLAN",
                    "targetUrl":"https://youtube.com/@new",
                    "brief":"Kế hoạch phát triển nội dung đáng tin cậy"}
                   """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Cache-Control","no-store"));
    }
}
