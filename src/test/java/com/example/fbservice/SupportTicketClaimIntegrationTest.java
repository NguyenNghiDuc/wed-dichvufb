package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.support.*;
import com.example.fbservice.notification.AccountNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:supportClaim;DB_CLOSE_DELAY=-1")
class SupportTicketClaimIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired SupportTicketService tickets;
    @Autowired AccountNotificationRepository notifications;

    private void staff(String username) {
        User u=new User();
        u.setUsername(username);
        u.setRole("STAFF");
        u.setEnabled(true);
        u.setPassword("$2a$10$fakeHashForTestsOnly");
        users.save(u);
    }

    @Test @WithMockUser(username="claim-staff-one",roles="STAFF")
    void onlyActiveStaffClaimsOnceAndOtherStaffCannotReplyOrClose() throws Exception {
        staff("claim-staff-one");
        staff("claim-staff-two");
        var ticket=tickets.create("claim-customer","Yêu cầu kiểm tra",
            "Xin nhân viên hỗ trợ phản hồi sự cố này.");
        String claim="/admin/support/"+ticket.getId()+"/claim";
        mvc.perform(post(claim)).andExpect(status().isForbidden());
        mvc.perform(post(claim).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(tickets.detailForAdmin(ticket.getId()).getAssigneeUsername())
            .isEqualTo("claim-staff-one");
        long firstCount=notifications.countByOwnerUsernameAndReadAtIsNull("claim-customer");
        mvc.perform(post(claim).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("claim-customer"))
            .isEqualTo(firstCount);
        mvc.perform(post(claim).with(user("claim-staff-two").roles("STAFF")).with(csrf()))
            .andExpect(status().isOk());
        assertThat(tickets.detailForAdmin(ticket.getId()).getAssigneeUsername())
            .isEqualTo("claim-staff-one");
        mvc.perform(post("/admin/support/"+ticket.getId()+"/reply")
                .with(user("claim-staff-two").roles("STAFF")).with(csrf())
                .param("message","Try reply"))
            .andExpect(status().isOk());
        assertThat(tickets.messagesForAdmin(ticket.getId())).isEmpty();
        mvc.perform(post("/admin/support/"+ticket.getId()+"/reply").with(csrf())
                .param("message","Đã tiếp nhận xử lý"))
            .andExpect(status().is3xxRedirection());
        assertThat(tickets.detailForAdmin(ticket.getId()).getStatus())
            .isEqualTo(TicketStatus.ANSWERED);
        mvc.perform(post("/admin/support/"+ticket.getId()+"/close")
                .with(user("claim-staff-two").roles("STAFF")).with(csrf()))
            .andExpect(status().isOk());
        assertThat(tickets.detailForAdmin(ticket.getId()).getStatus())
            .isEqualTo(TicketStatus.ANSWERED);
        mvc.perform(post("/admin/support/"+ticket.getId()+"/close").with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(tickets.detailForAdmin(ticket.getId()).getStatus())
            .isEqualTo(TicketStatus.CLOSED);
        // Same employee must not revive or acknowledge a claim on a closed ticket.
        mvc.perform(post(claim).with(csrf())).andExpect(status().isOk());
        assertThat(tickets.detailForAdmin(ticket.getId()).getStatus())
            .isEqualTo(TicketStatus.CLOSED);
    }

    @Test @WithMockUser(username="claim-ordinary",roles="USER")
    void ordinaryUsersCannotClaimEvenWithCsrfOrDirectServiceCalls() throws Exception {
        var ticket=tickets.create("claim-second-customer","Vấn đề dịch vụ",
            "Yêu cầu nhân viên hỗ trợ để kiểm tra lỗi.");
        mvc.perform(post("/admin/support/"+ticket.getId()+"/claim").with(csrf()))
            .andExpect(status().isForbidden());
        assertThatThrownBy(() -> tickets.claimAsStaff("claim-ordinary",ticket.getId()))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
