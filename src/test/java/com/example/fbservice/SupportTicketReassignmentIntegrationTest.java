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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:supportReassign27;DB_CLOSE_DELAY=-1")
class SupportTicketReassignmentIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketService tickets;
    @Autowired UserRepository users;
    @Autowired AccountNotificationRepository notices;

    private User account(String name, String role, boolean active) {
        User u=new User();
        u.setUsername(name);
        u.setRole(role);
        u.setEnabled(active);
        u.setPassword("$2a$10$testOnlyHashForSupportReview");
        return users.save(u);
    }

    @Test @WithMockUser(username="reassign-admin",roles="ADMIN")
    void adminTransferIsAuditedAndOnlyAssigneeCanReply() throws Exception {
        account("reassign-admin","ADMIN",true);
        account("reassign-old","STAFF",true);
        User worker=account("reassign-new","STAFF",true);
        User inactive=account("reassign-inactive","STAFF",false);
        User client=account("reassign-customer","USER",true);
        var ticket=tickets.create(client.getUsername(),"Chuyển giao hỗ trợ",
            "Yêu cầu thay đổi người phụ trách do có điều chỉnh lịch.");
        tickets.claimAsStaff("reassign-old",ticket.getId());
        assertThat(tickets.assignmentHistory(ticket.getId())).hasSize(1);
        String url="/admin/support/"+ticket.getId()+"/reassign";
        mvc.perform(post(url).param("employeeId",worker.getId().toString()))
            .andExpect(status().isForbidden());
        mvc.perform(post(url).with(user("reassign-old").roles("STAFF")).with(csrf())
                .param("employeeId",worker.getId().toString()))
            .andExpect(status().isForbidden());
        mvc.perform(post(url).with(csrf()).param("employeeId", inactive.getId().toString()))
            .andExpect(status().isOk());
        assertThat(tickets.detailForAdmin(ticket.getId()).getAssigneeUsername()).isEqualTo("reassign-old");
        mvc.perform(post(url).with(csrf()).param("employeeId",worker.getId().toString()))
            .andExpect(status().is3xxRedirection());
        assertThat(tickets.detailForAdmin(ticket.getId()).getAssigneeUsername()).isEqualTo("reassign-new");
        assertThat(tickets.assignmentHistory(ticket.getId())).hasSize(2);
        assertThat(tickets.assignmentHistory(ticket.getId()).get(0).getOldAssignee()).isEqualTo("reassign-old");
        assertThat(tickets.assignmentHistory(ticket.getId()).get(0).getActor()).isEqualTo("reassign-admin");
        assertThat(notices.countByOwnerUsernameAndReadAtIsNull("reassign-new")).isEqualTo(1);
        mvc.perform(post("/admin/support/"+ticket.getId()+"/reply")
                .with(user("reassign-old").roles("STAFF")).with(csrf())
                .param("message","Unauthorized after transfer")).andExpect(status().isOk());
        assertThat(tickets.messagesForAdmin(ticket.getId())).isEmpty();
        mvc.perform(post("/admin/support/"+ticket.getId()+"/reply")
                .with(user("reassign-new").roles("STAFF")).with(csrf())
                .param("message","Đã nhận chuyển giao xử lý."))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post(url).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(tickets.detailForAdmin(ticket.getId()).getAssigneeUsername()).isNull();
        assertThat(tickets.assignmentHistory(ticket.getId())).hasSize(3);
    }

    @Test @WithMockUser(username="reassign-notification-staff",roles="STAFF")
    void customerReplyNotifiesAssignedStaffButNeverCopiesMessageBody() throws Exception {
        account("reassign-notification-staff","STAFF",true);
        var ticket=tickets.create("reassign-notification-owner","Hỗ trợ thanh toán nội dung",
            "Khách hàng gửi phản hồi để nhân viên xử lý yêu cầu.");
        tickets.claimAsStaff("reassign-notification-staff",ticket.getId());
        String unsafe = "<script>alert('private')</script>";
        tickets.replyAsOwner("reassign-notification-owner",ticket.getId(),unsafe);
        assertThat(notices.countByOwnerUsernameAndReadAtIsNull("reassign-notification-staff")).isEqualTo(1);
        var message = notices.findTop100ByOwnerUsernameOrderByCreatedAtDesc(
            "reassign-notification-staff").get(0).getMessage();
        assertThat(message).doesNotContain("<script>").doesNotContain("private");
        assertThat(tickets.detailForOwner("reassign-notification-owner",ticket.getId()).getStatus())
            .isEqualTo(TicketStatus.OPEN);
    }

    @Test @WithMockUser(username="reassign-other-admin",roles="ADMIN")
    void preventsDuplicateTransferAndClosedTicketChanges() throws Exception {
        account("reassign-other-admin","ADMIN",true);
        var ticket=tickets.create("reassign-other-customer","Bàn giao nhân viên",
            "Nội dung để kiểm tra quy tắc tiếp nhận và đóng yêu cầu.");
        assertThatThrownBy(()->tickets.reassignAsAdmin("reassign-other-admin",ticket.getId(),null))
            .isInstanceOf(IllegalArgumentException.class);
        tickets.closeAsAdmin("reassign-other-admin",ticket.getId());
        assertThatThrownBy(()->tickets.reassignAsAdmin("reassign-other-admin",ticket.getId(),null))
            .isInstanceOf(IllegalArgumentException.class);
        mvc.perform(get("/admin/support/"+ticket.getId()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("canReassign",true));
        mvc.perform(get("/admin/support/"+ticket.getId()).with(user("reassign-other-staff").roles("STAFF")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("canReassign",false));
    }
}
