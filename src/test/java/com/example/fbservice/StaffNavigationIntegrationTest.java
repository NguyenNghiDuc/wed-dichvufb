package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:staffnavigationtest;DB_CLOSE_DELAY=-1")
class StaffNavigationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    private void create(String username, String role){
        User account=new User();
        account.setUsername(username);
        account.setRole(role);
        account.setPassword("test fixture, not for login");
        users.save(account);
    }

    @Test
    @WithMockUser(username="staff-navigation",roles="STAFF")
    void staffSeesAssignedOrdersLink() throws Exception {
        create("staff-navigation","STAFF");
        mvc.perform(get("/profile")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/staff/campaign-orders")));
    }

    @Test
    @WithMockUser(username="customer-navigation")
    void customerDoesNotSeeStaffLink() throws Exception {
        create("customer-navigation","USER");
        mvc.perform(get("/profile")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("/staff/campaign-orders"))));
        mvc.perform(get("/staff/campaign-orders")).andExpect(status().isForbidden());
    }
}
