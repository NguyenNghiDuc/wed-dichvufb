package com.example.fbservice;

import com.example.fbservice.admin.AdminFirstOrderCustomerService;
import com.example.fbservice.campaign.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4firstordercustomers;DB_CLOSE_DELAY=-1")
class AdminFirstOrderCustomerIntegrationTest {
    @Autowired AdminFirstOrderCustomerService firstOrders;
    @Autowired CampaignOrderService orders;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    private void create(String owner,LocalDateTime date) {
        var order=orders.create(owner,new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@trusted","Chiến dịch truyền thông hợp pháp của khách."));
        jdbc.update("update campaign_orders set created_at=? where id=?",
            Timestamp.valueOf(date), order.getId());
    }

    @Test @WithMockUser(username="w4-customer-count-admin",roles="ADMIN")
    void uniqueFirstOrderCustomersExcludeOlderOwnersAndMultipleOrders() throws Exception {
        LocalDateTime now=LocalDateTime.now();
        create("w4-first-only",now.minusDays(2));
        create("w4-first-only",now.minusHours(1));
        create("w4-existing-owner",now.minusDays(90));
        create("w4-existing-owner",now.minusDays(1));
        create("w4-other-new-owner",now.minusDays(3));
        assertThat(firstOrders.lastThirtyDays()).isEqualTo(2L);

        mvc.perform(get("/admin/dashboard")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Khách lần đầu có đơn")))
            .andExpect(content().string(containsString("Không phải số tài khoản mới đăng ký")));
        mvc.perform(get("/api/v1/admin/analytics/first-order-customers"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.customers").value(2))
            .andExpect(jsonPath("$.definition",containsString("not registrations")));
    }

    @Test void thisCustomerMetricIsNotPublic() throws Exception {
        mvc.perform(get("/api/v1/admin/analytics/first-order-customers"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/v1/admin/analytics/first-order-customers")
            .with(user("w4-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").with(user("w4-customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
