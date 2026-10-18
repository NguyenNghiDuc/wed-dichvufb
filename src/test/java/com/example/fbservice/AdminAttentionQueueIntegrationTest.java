package com.example.fbservice;

import com.example.fbservice.admin.AdminAttentionQueueService;
import com.example.fbservice.campaign.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4attentionqueue;DB_CLOSE_DELAY=-1")
class AdminAttentionQueueIntegrationTest {
    @Autowired AdminAttentionQueueService queue;
    @Autowired CampaignOrderRepository orders;
    @Autowired MockMvc mvc;

    // Independent fixtures: pagination tests insert old orders into the same H2 database.
    @BeforeEach
    void clearTestOrders() { orders.deleteAll(); }

    private CampaignOrder create(String owner, CampaignStatus status,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        CampaignOrder o = new CampaignOrder(owner,
            new CampaignOrderRequest(SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
                "https://youtube.com/@growlab", "Tư vấn nội dung phù hợp chính sách mạng xã hội."), 79000);
        if(status == CampaignStatus.APPROVED || status == CampaignStatus.IN_PROGRESS)
            o.changeStatus(CampaignStatus.APPROVED);
        if(status == CampaignStatus.IN_PROGRESS) o.changeStatus(CampaignStatus.IN_PROGRESS);
        if(status == CampaignStatus.REJECTED) o.changeStatus(CampaignStatus.REJECTED);
        ReflectionTestUtils.setField(o, "createdAt", createdAt);
        ReflectionTestUtils.setField(o, "updatedAt", updatedAt);
        return orders.save(o);
    }

    @Test
    @WithMockUser(username="w4-queue-admin",roles="ADMIN")
    void priorityQueuesOnlyContainMatchingAndActuallyOverdueOrders() throws Exception {
        var now = LocalDateTime.now();
        CampaignOrder oldPending = create("w4-old-pending", CampaignStatus.PENDING_REVIEW,
            now.minusHours(72), now.minusHours(72));
        create("w4-new-pending", CampaignStatus.PENDING_REVIEW, now.minusHours(4), now.minusHours(4));
        CampaignOrder oldWork = create("w4-old-work", CampaignStatus.IN_PROGRESS,
            now.minusDays(12), now.minusDays(9));
        create("w4-updated-work", CampaignStatus.IN_PROGRESS,
            now.minusDays(13), now.minusHours(3));
        CampaignOrder ready = create("w4-ready", CampaignStatus.APPROVED,
            now.minusHours(5), now.minusHours(5));
        CampaignOrder rejected = create("w4-rejected", CampaignStatus.REJECTED,
            now.minusHours(9), now.minusHours(9));

        assertThat(queue.find(AdminAttentionQueueService.Queue.OVERDUE_PENDING, 0).rows())
            .extracting(CampaignOrder::getId).containsExactly(oldPending.getId());
        assertThat(queue.find(AdminAttentionQueueService.Queue.OVERDUE_IN_PROGRESS, 0).rows())
            .extracting(CampaignOrder::getId).containsExactly(oldWork.getId());
        assertThat(queue.find(AdminAttentionQueueService.Queue.READY_TO_ASSIGN, 0).rows())
            .extracting(CampaignOrder::getId).containsExactly(ready.getId());
        assertThat(queue.find(AdminAttentionQueueService.Queue.REJECTED, 0).rows())
            .extracting(CampaignOrder::getId).containsExactly(rejected.getId());

        mvc.perform(get("/admin/attention").param("queue", "OVERDUE_PENDING"))
            .andExpect(status().isOk()).andExpect(view().name("admin_attention"))
            .andExpect(model().attribute("orders", hasSize(1)))
            .andExpect(content().string(containsString("w4-old-pending")));
    }

    @Test
    @WithMockUser(username="w4-queue-admin-pages",roles="ADMIN")
    void queueUsesStableSqlPagingAndRejectsMalformedParameters() throws Exception {
        LocalDateTime old=LocalDateTime.now().minusHours(80);
        for(int i=0;i<25;i++)
            create("w4-page-"+i, CampaignStatus.PENDING_REVIEW, old.minusMinutes(i), old.minusMinutes(i));
        var first=queue.find(AdminAttentionQueueService.Queue.OVERDUE_PENDING,0);
        var second=queue.find(AdminAttentionQueueService.Queue.OVERDUE_PENDING,1);
        assertThat(first.rows()).hasSize(20);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.rows()).hasSize(5);
        assertThat(second.hasNext()).isFalse();
        assertThat(first.rows()).extracting(CampaignOrder::getId)
            .doesNotContainAnyElementsOf(second.rows().stream().map(CampaignOrder::getId).toList());
        mvc.perform(get("/admin/attention").param("queue","NOT_A_QUEUE"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/attention").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/attention").param("page","10001"))
            .andExpect(status().isBadRequest());
    }


    @Test
    @WithMockUser(roles="ADMIN")
    void unavailableAssigneeQueueFindsMissingStaff() throws Exception {
        CampaignOrder order = create("w4-missing-owner", CampaignStatus.APPROVED,
            LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
        order.assignTo("w4-missing-employee");
        orders.save(order);
        assertThat(queue.find(AdminAttentionQueueService.Queue.UNAVAILABLE_ASSIGNEE, 0).rows())
            .extracting(CampaignOrder::getId).containsExactly(order.getId());
        mvc.perform(get("/admin/attention").param("queue", "UNAVAILABLE_ASSIGNEE"))
            .andExpect(status().isOk()).andExpect(model().attribute("orders", hasSize(1)));
    }

    @Test
    @WithMockUser(roles="ADMIN")
    void staleAssignedApprovedQueueExcludesFreshUnassignedAndInProgressOrders() throws Exception {
        var now = LocalDateTime.now();
        CampaignOrder stale = create("w4-stale-assigned", CampaignStatus.APPROVED,
            now.minusDays(8), now.minusDays(5));
        stale.assignTo("w4-staff-stale");
        orders.save(stale);
        CampaignOrder fresh = create("w4-fresh-assigned", CampaignStatus.APPROVED,
            now.minusDays(5), now.minusHours(1));
        fresh.assignTo("w4-staff-fresh");
        orders.save(fresh);
        create("w4-old-unassigned", CampaignStatus.APPROVED,
            now.minusDays(6), now.minusDays(5));
        CampaignOrder running = create("w4-old-running", CampaignStatus.IN_PROGRESS,
            now.minusDays(9), now.minusDays(8));
        running.assignTo("w4-staff-running");
        orders.save(running);

        assertThat(queue.find(AdminAttentionQueueService.Queue.STALE_ASSIGNED_APPROVED, 0).rows())
            .extracting(CampaignOrder::getId).containsExactly(stale.getId());
        mvc.perform(get("/admin/attention").param("queue", "STALE_ASSIGNED_APPROVED"))
            .andExpect(status().isOk()).andExpect(model().attribute("orders", hasSize(1)))
            .andExpect(content().string(containsString("w4-stale-assigned")));
    }

    @Test
    void onlyAdminCanViewPriorityQueue() throws Exception {
        mvc.perform(get("/admin/attention")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/attention").with(user("w4-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/attention").with(user("w4-client").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
