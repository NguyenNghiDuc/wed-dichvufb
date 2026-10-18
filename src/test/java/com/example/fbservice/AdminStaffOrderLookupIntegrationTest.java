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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4stafforderlookup;DB_CLOSE_DELAY=-1")
class AdminStaffOrderLookupIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CampaignOrderService campaigns;
    @Autowired CampaignAssignmentService assignments;
    @Autowired UserRepository users;

    private User account(String username,String role) {
        User u=new User();u.setUsername(username);u.setRole(role);
        u.setEnabled(true);u.setPassword("test-only-placeholder");
        return users.save(u);
    }
    private CampaignOrder campaign(String owner) {
        return campaigns.create(owner,new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@trusted","Nội dung thử nghiệm phù hợp chính sách."));
    }

    @Test @WithMockUser(username="w4-search-worker1",roles="STAFF")
    void staffCanFindOnlyOwnAssignedOrderByIdAndStatus() throws Exception {
        User admin=account("w4-search-operator","ADMIN");
        User worker1=account("w4-search-worker1","STAFF");
        User worker2=account("w4-search-worker2","STAFF");
        CampaignOrder mine=campaign("w4-search-customer1");
        CampaignOrder other=campaign("w4-search-customer2");
        assignments.assign(mine.getId(),worker1.getId(),admin.getUsername());
        assignments.assign(other.getId(),worker2.getId(),admin.getUsername());
        campaigns.updateStatusForAdmin(mine.getId(),CampaignStatus.APPROVED,admin.getUsername());

        mvc.perform(get("/staff/campaign-orders").param("orderId",mine.getId().toString())
                .param("status","APPROVED"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders",hasSize(1)))
            .andExpect(content().string(containsString("w4-search-customer1")))
            .andExpect(content().string(not(containsString("w4-search-customer2"))));
        mvc.perform(get("/staff/campaign-orders").param("orderId",other.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders",hasSize(0)));
        mvc.perform(get("/staff/campaign-orders").param("orderId",mine.getId().toString())
                .param("status","COMPLETED"))
            .andExpect(model().attribute("orders",hasSize(0)));
        mvc.perform(get("/staff/campaign-orders").param("orderId","0"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/staff/campaign-orders/"+other.getId()+"/progress").with(csrf())
                .param("status","IN_PROGRESS"))
            .andExpect(status().isNotFound());
    }

    @Test void userAndAnonymousCannotSearchStaffWork() throws Exception {
        mvc.perform(get("/staff/campaign-orders").param("orderId","1"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/staff/campaign-orders").with(user("w4-search-customer").roles("USER"))
                .param("orderId","1"))
            .andExpect(status().isForbidden());
    }
}
