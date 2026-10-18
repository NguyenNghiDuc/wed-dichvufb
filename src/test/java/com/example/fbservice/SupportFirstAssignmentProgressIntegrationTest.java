package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.support.SupportTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:w3assignmentprogress;DB_CLOSE_DELAY=-1")
class SupportFirstAssignmentProgressIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketService tickets;
    @Autowired UserRepository users;

    @Test
    @WithMockUser(username = "first-assigned-owner")
    void firstStaffAcceptanceUsesRealAssignmentAuditWithoutLeakingOtherTickets() throws Exception {
        var mine = tickets.create("first-assigned-owner", "Cần nhân viên tiếp nhận",
                "Tôi muốn biết thời gian nhân viên tiếp nhận ticket.");
        var foreign = tickets.create("another-owner-only",
                "Ticket hoàn toàn riêng tư", "Không ai ngoài chủ ticket được xem.");
        String path = "/support/" + mine.getId() + "/progress";

        mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned").value(false))
                .andExpect(jsonPath("$.firstAssignedAt").doesNotExist())
                .andExpect(jsonPath("$.minutesToFirstAssignment").doesNotExist());

        User staff = new User();
        staff.setUsername("first-assigned-employee");
        staff.setRole("STAFF");
        staff.setEnabled(true);
        staff.setPassword("$2a$10$fakePasswordHashForTestOnly");
        users.saveAndFlush(staff);
        tickets.claimAsStaff("first-assigned-employee", mine.getId());

        mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.assigned").value(true))
                .andExpect(jsonPath("$.firstAssignedAt").exists())
                .andExpect(jsonPath("$.minutesToFirstAssignment").isNumber());
        assertThat(tickets.progressForOwner("first-assigned-owner", mine.getId())
                .minutesToFirstAssignment()).isGreaterThanOrEqualTo(0L);
        mvc.perform(get("/support/" + mine.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tiếp nhận lần đầu")))
                .andExpect(content().string(containsString("Thời gian tới lúc tiếp nhận")));
        mvc.perform(get("/support/" + foreign.getId() + "/progress"))
                .andExpect(status().isNotFound());
        mvc.perform(get(path).with(user("another-owner-only")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/admin/support/" + mine.getId() + "/progress")
                        .with(user("first-assigned-employee").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstAssignedAt").exists());
    }
}
