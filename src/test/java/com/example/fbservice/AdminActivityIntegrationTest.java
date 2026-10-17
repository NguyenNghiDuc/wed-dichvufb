package com.example.fbservice;

import com.example.fbservice.admin.*;
import com.example.fbservice.campaign.*;
import com.example.fbservice.catalog.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4unifiedactivity;DB_CLOSE_DELAY=-1")
class AdminActivityIntegrationTest {
    @Autowired AdminActivityService service;
    @Autowired AdminAccountAuditRepository accounts;
    @Autowired ServiceCatalogAuditRepository pricing;
    @Autowired CampaignEventRepository campaignEvents;
    @Autowired CampaignAssignmentEventRepository assignments;
    @Autowired CampaignOrderService orders;
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username="w4-activity-admin",roles="ADMIN")
    void collectsOnlyAdminOperationsAcrossFourExistingSources() throws Exception {
        accounts.save(new AdminAccountAudit("w4-activity-admin", 500L,
                "ACCOUNT_ROLE", "USER", "STAFF"));
        pricing.save(new ServiceCatalogAudit("w4-activity-admin", "CONTENT_PLAN",
                15000, 20000, true, true));
        CampaignOrder order = orders.create("w4-activity-owner",
            new CampaignOrderRequest(SocialPlatform.YOUTUBE,
                CampaignServiceType.CONTENT_PLAN, "https://youtube.com/@growlab",
                "Dự án tư vấn nội dung có chính sách rõ ràng."));
        assignments.save(new CampaignAssignmentEvent(order.getId(),
                null, "w4-activity-staff", "w4-activity-admin"));
        orders.updateStatusForAdmin(order.getId(), CampaignStatus.APPROVED, "w4-activity-admin");
        List<AdminActivityService.Entry> entries = service.recent(AdminActivityService.Area.ALL);
        assertThat(entries).extracting(AdminActivityService.Entry::area)
            .contains(AdminActivityService.Area.ACCOUNTS,
                AdminActivityService.Area.CATALOG, AdminActivityService.Area.ASSIGNMENTS,
                AdminActivityService.Area.ORDER_STATUS);
        assertThat(entries).noneMatch(e -> e.details().contains("Chiến dịch được tạo"));
        assertThat(service.recent(AdminActivityService.Area.ACCOUNTS))
            .allMatch(e -> e.area() == AdminActivityService.Area.ACCOUNTS);
        mvc.perform(get("/admin/activity")).andExpect(status().isOk())
            .andExpect(view().name("admin_activity"))
            .andExpect(model().attributeExists("entries"))
            .andExpect(content().string(containsString("Nhật ký hoạt động Admin")));
        mvc.perform(get("/admin/activity").param("area","CATALOG"))
            .andExpect(status().isOk()).andExpect(model().attribute("entries",hasSize(1)));
        mvc.perform(get("/admin/activity").param("area","INVALID"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username="w4-activity-cap",roles="ADMIN")
    void recentActivityIsBoundedAndNeverLoadsEntireAuditTable() {
        for(int i=0;i<65;i++)
            accounts.save(new AdminAccountAudit("w4-activity-cap", (long)i+10000,
                    "ACCOUNT_ENABLED", "true", "false"));
        assertThat(service.recent(AdminActivityService.Area.ACCOUNTS)).hasSize(50);
        assertThat(service.recent(AdminActivityService.Area.ALL)).hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void activityIsOnlyAccessibleToAdminsAndNeverAcceptsWrites() throws Exception {
        mvc.perform(get("/admin/activity")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/activity").with(user("w4-activity-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/activity").with(user("w4-activity-customer").roles("USER")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/activity").with(user("w4-activity-admin").roles("ADMIN")))
            .andExpect(status().isForbidden()); // CSRF denies untrusted mutation.
    }
}
