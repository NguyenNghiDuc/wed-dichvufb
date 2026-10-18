package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:assignmenthistoryw4;DB_CLOSE_DELAY=-1")
class AdminAssignmentHistoryPagingTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CampaignOrderRepository orders;
    @Autowired CampaignAssignmentEventRepository events;
    @Autowired CampaignAssignmentService assignments;

    @Test @WithMockUser(username="history-operator",roles="ADMIN")
    void boundedHistoryAndAdminAuthorization() throws Exception {
        User admin = new User();
        admin.setUsername("history-operator");admin.setRole("ADMIN");
        admin.setPassword("unused-test-hash");users.save(admin);
        CampaignOrder order = new CampaignOrder("history-owner",new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@growlab", "Tư vấn kế hoạch nội dung hợp lệ."),79000);
        Long id=orders.save(order).getId();
        for(int i=0;i<25;i++)
            events.save(new CampaignAssignmentEvent(id,null,"history-worker","history-operator"));
        var page0=assignments.pageAssignmentHistory("history-operator",id,0);
        var page1=assignments.pageAssignmentHistory("history-operator",id,1);
        assertThat(page0.getContent()).hasSize(20);
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page0.getContent()).extracting(CampaignAssignmentEvent::getId)
            .doesNotContainAnyElementsOf(page1.getContent().stream().map(CampaignAssignmentEvent::getId).toList());
        mvc.perform(get("/admin/campaign-orders/"+id+"/assignment-history").param("page","1"))
            .andExpect(status().isOk()).andExpect(model().attribute("events",hasSize(5)))
            .andExpect(view().name("admin_assignment_history"));
        mvc.perform(get("/admin/campaign-orders/"+id+"/assignment-history").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/campaign-orders/"+id+"/assignment-history").param("page","10001"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/campaign-orders/999999/assignment-history"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/admin/campaign-orders/"+id+"/assignment-history")
            .with(user("staff-worker").roles("STAFF"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/campaign-orders/"+id+"/assignment-history")
            .with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
    }
}
