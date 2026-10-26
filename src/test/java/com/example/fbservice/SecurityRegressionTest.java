package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.config.TrafficLimiter;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:securityregression;DB_CLOSE_DELAY=-1")
class SecurityRegressionTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;
    @Autowired TrafficLimiter limiter;

    @Test
    void protectiveHeadersAndRateLimiterPresent() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
           .andExpect(header().string("Content-Security-Policy",
               org.hamcrest.Matchers.containsString("default-src 'self'")));

        limiter.check("quality-gate", 2, Duration.ofMinutes(1));
        limiter.check("quality-gate", 2, Duration.ofMinutes(1));
        assertThatThrownBy(() -> limiter.check("quality-gate", 2, Duration.ofMinutes(1)))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void staleAuthenticationFormsAreRejectedAndOfferFreshSameOriginForms() throws Exception {
        for (String path : new String[]{"/login", "/register"}) {
            mvc.perform(post(path).param("username", "stale_csrf_account").param("password", "UnusedPass123!"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", path + "?sessionExpired"));
            mvc.perform(post(path).with(csrf().useInvalidToken()))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", path + "?sessionExpired"));
            mvc.perform(get(path).param("sessionExpired", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Phiên xác thực đã hết hạn")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        }
        mvc.perform(post("/profile/display-name").param("displayName", "Blocked"))
            .andExpect(status().isForbidden());
        var fresh = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        var token = java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
            .matcher(fresh.getResponse().getContentAsString());
        assertThat(token.find()).isTrue();
        mvc.perform(post("/login")
                .session((org.springframework.mock.web.MockHttpSession) fresh.getRequest().getSession(false))
                .param("_csrf", token.group(1))
                .param("username", "csrf_recovery_unknown_user").param("password", "NotThePassword123!"))
            .andExpect(status().isFound()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void excessiveLoginAttemptsRespond429() throws Exception {
        for (int index = 0; index < 10; index++) {
            mvc.perform(post("/login").with(csrf())
                .with(request -> { request.setRemoteAddr("198.51.100.55"); return request; })
                .param("username", "nonexistent-user-rate-limit")
                .param("password", "incorrect-password"))
                .andExpect(status().is3xxRedirection());
        }
        mvc.perform(post("/login").with(csrf())
             .with(request -> { request.setRemoteAddr("198.51.100.55"); return request; })
             .param("username", "nonexistent-user-rate-limit")
             .param("password", "incorrect-password"))
           .andExpect(status().isTooManyRequests())
           .andExpect(header().string("Retry-After", "600"));
    }

    @Test
    @WithMockUser(username = "quality-owner")
    void onlyOwnerCanCancelPendingOrderAndCsrfIsRequired() throws Exception {
        CampaignOrder order = campaigns.create("quality-owner",
            new CampaignOrderRequest(SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
                "https://youtube.com/@official", "Kế hoạch nội dung hữu ích, không dùng tương tác giả."));
        long id = order.getId();
        mvc.perform(post("/api/v1/campaign-orders/" + id + "/cancel"))
           .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/campaign-orders/" + id + "/cancel")
                .with(user("outsider")).with(csrf()))
           .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/campaign-orders/" + id + "/cancel").with(csrf()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/v1/campaign-orders/" + id + "/cancel").with(csrf()))
           .andExpect(status().isBadRequest());
        assertThat(campaigns.historyForOwner("quality-owner", id)).hasSize(2);
    }

    @Test
    @WithMockUser(username = "pagination-owner")
    void pageIsOwnerScopedAndInputBounded() throws Exception {
        campaigns.create("pagination-owner", new CampaignOrderRequest(SocialPlatform.TIKTOK,
            CampaignServiceType.PROFILE_AUDIT,"https://tiktok.com/@valid",
            "Đánh giá kênh kinh doanh về đồ dùng học tập."));
        mvc.perform(get("/api/v1/campaign-orders/page").param("page", "0").param("size","1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content[0].ownerUsername").value("pagination-owner"));
        mvc.perform(get("/api/v1/campaign-orders/page").param("page","0").param("size","1000"))
           .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").with(user("outsider")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content").isEmpty());
    }
    @Test void separateAccountsBehindOnePeerDoNotShareTheSmallLoginBudget() throws Exception {
        for(int i=0;i<12;i++){
            mvc.perform(post("/login").with(csrf()).with(r->{r.setRemoteAddr("198.51.100.90");return r;})
                .param("username","different_name_"+i).param("password","not-a-password"))
                .andExpect(status().is3xxRedirection());
        }
    }

}
