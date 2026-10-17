package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.support.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:supportQueue27;DB_CLOSE_DELAY=-1")
class SupportTicketAssignmentQueueIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketService tickets;
    @Autowired UserRepository users;

    private void activeStaff(String name) {
        User u=new User();
        u.setUsername(name);
        u.setPassword("$2a$10$testHashQueue");
        u.setRole("STAFF");
        u.setEnabled(true);
        users.save(u);
    }

    @Test @WithMockUser(username="queue-staff-one",roles="STAFF")
    void queueIsBoundedAndCannotFakeOtherAssignees() throws Exception {
        activeStaff("queue-staff-one");
        activeStaff("queue-staff-two");
        Long mineId=null, otherId=null;
        for(int i=0;i<23;i++){
            var ticket=tickets.create("queue-customer-"+i,"Yêu cầu queue "+i,
                "Khách hàng cần hỗ trợ với chiến dịch dịch vụ.");
            if(i==0) {
                mineId=ticket.getId();
                tickets.claimAsStaff("queue-staff-one",mineId);
            } else if (i==1) {
                tickets.claimAsStaff("queue-staff-one",ticket.getId());
            } else if (i==2) {
                otherId=ticket.getId();
                tickets.claimAsStaff("queue-staff-two",otherId);
            }
        }
        mvc.perform(get("/admin/support").param("assignment","MINE"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("tickets",hasSize(2)))
            .andExpect(model().attribute("selectedAssignment", TicketAssignmentFilter.MINE))
            .andExpect(content().string(containsString("name=\"assignment\"")));
        mvc.perform(get("/admin/support").param("assignment","UNASSIGNED"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("tickets",hasSize(20)))
            .andExpect(model().attribute("hasNext",false));
        mvc.perform(get("/admin/support").param("assignment","ALL"))
            .andExpect(model().attribute("tickets",hasSize(20)))
            .andExpect(model().attribute("hasNext",true));
        mvc.perform(get("/admin/support").with(user("queue-staff-two").roles("STAFF"))
            .param("assignment","MINE"))
            .andExpect(model().attribute("tickets",hasSize(1)));
        mvc.perform(get("/admin/support").param("assignment","fake-person"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/support").with(user("queue-stranger"))
            .param("assignment","MINE"))
            .andExpect(status().isForbidden());
        assertThat(tickets.detailForAdmin(mineId).getAssigneeUsername()).isEqualTo("queue-staff-one");
        assertThat(tickets.detailForAdmin(otherId).getAssigneeUsername()).isEqualTo("queue-staff-two");
    }
}
