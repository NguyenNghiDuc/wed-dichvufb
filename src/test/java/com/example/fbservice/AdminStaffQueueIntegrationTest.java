package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4staffqueues;DB_CLOSE_DELAY=-1")
class AdminStaffQueueIntegrationTest {
    @Autowired UserRepository users;
    @Autowired CampaignOrderService campaigns;
    @Autowired CampaignAssignmentService assignments;
    @Autowired CampaignOrderRepository orders;
    @Autowired MockMvc mvc;

    private User createUser(String username, String role) {
        User u = new User();u.setUsername(username);u.setRole(role);
        u.setPassword("$2a$10$test-not-a-live-password");
        return users.save(u);
    }
    private CampaignOrder campaign(String username) {
        return campaigns.create(username, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@growlab", "Nội dung hữu ích cho khách hàng."));
    }

    @Test
    @WithMockUser(username="w4-assigned-a",roles="STAFF")
    void staffOnlySeesOwnOrdersEvenWhenFilteringStatus() throws Exception {
        createUser("w4-assignment-admin", "ADMIN");
        User staffA = createUser("w4-assigned-a", "STAFF");
        User staffB = createUser("w4-assigned-b", "STAFF");
        CampaignOrder first = campaign("w4-customer-a");
        CampaignOrder second = campaign("w4-customer-b");
        campaigns.updateStatusForAdmin(first.getId(), CampaignStatus.APPROVED,"w4-assignment-admin");
        campaigns.updateStatusForAdmin(second.getId(), CampaignStatus.APPROVED,"w4-assignment-admin");
        assignments.assign(first.getId(),staffA.getId(),"w4-assignment-admin");
        assignments.assign(second.getId(),staffB.getId(),"w4-assignment-admin");

        mvc.perform(get("/staff/campaign-orders").param("status","APPROVED"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders", hasSize(1)))
            .andExpect(model().attribute("selectedStatus", CampaignStatus.APPROVED));
        mvc.perform(get("/staff/campaign-orders").param("status","IN_PROGRESS"))
            .andExpect(status().isOk()).andExpect(model().attribute("orders",empty()));
        mvc.perform(get("/staff/campaign-orders").with(user("w4-assigned-b").roles("STAFF"))
                .param("status","APPROVED"))
            .andExpect(status().isOk()).andExpect(model().attribute("orders",hasSize(1)));
        mvc.perform(post("/staff/campaign-orders/" + second.getId() + "/progress")
                .with(csrf()).param("status", "IN_PROGRESS"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/staff/campaign-orders/" + first.getId() + "/progress")
                .param("status","IN_PROGRESS"))
            .andExpect(status().isForbidden());
        assertThat(orders.findById(first.getId()).orElseThrow().getStatus())
            .isEqualTo(CampaignStatus.APPROVED);
    }

    @Test
    @WithMockUser(username="w4-validation-staff", roles="STAFF")
    void invalidPagingStatusAndUnregisteredStaffAreRejected() throws Exception {
        createUser("w4-validation-staff","STAFF");
        mvc.perform(get("/staff/campaign-orders").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/staff/campaign-orders").param("page","10001"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/staff/campaign-orders").param("status","INVALID"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/staff/campaign-orders").with(user("missing-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
    }

    @Test
    void userAndAnonymousCannotInspectStaffWorkQueues() throws Exception {
        mvc.perform(get("/staff/campaign-orders")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/staff/campaign-orders").with(user("w4-customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
