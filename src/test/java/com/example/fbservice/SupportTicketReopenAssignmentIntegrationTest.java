package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.support.SupportTicketService;
import com.example.fbservice.support.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:w3reopenassignee;DB_CLOSE_DELAY=-1")
class SupportTicketReopenAssignmentIntegrationTest {
    @Autowired SupportTicketService tickets;
    @Autowired UserRepository users;

    private User staff(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEnabled(true);
        user.setRole("STAFF");
        user.setPassword("$2a$10$fakeTestOnlyStaffCredential");
        return users.saveAndFlush(user);
    }

    @Test
    void reopeningAnInactiveAssigneeReturnsTicketToQueueWithAnAuditedUnassignment() {
        User stale = staff("reopen-stale-staff");
        var ticket = tickets.create("reopen-stale-owner", "Ticket cần chuyển giao",
                "Nhân viên cũ đã ngừng hoạt động trong thời gian đóng yêu cầu.");
        Long id = ticket.getId();
        tickets.claimAsStaff(stale.getUsername(), id);
        tickets.closeAsAdmin(stale.getUsername(), id);
        stale.setEnabled(false);
        users.saveAndFlush(stale);

        tickets.reopenAsOwner("reopen-stale-owner", id,
                "Nhân viên cũ không còn tiếp tục xử lý, xin hỗ trợ lại.");
        var reopened = tickets.detailForOwner("reopen-stale-owner", id);
        assertThat(reopened.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(reopened.getAssigneeUsername()).isNull();
        var history = tickets.assignmentHistory(id);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getOldAssignee()).isEqualTo("reopen-stale-staff");
        assertThat(history.get(0).getNewAssignee()).isNull();
        assertThat(history.get(0).getActor()).isEqualTo("HỆ THỐNG");
        assertThat(tickets.messagesForOwner("reopen-stale-owner", id)).hasSize(3);
    }

    @Test
    void reopeningAStillActiveAssigneePreservesTheExistingAssignment() {
        User current = staff("reopen-active-staff");
        var ticket = tickets.create("reopen-active-owner", "Tiếp tục yêu cầu",
                "Yêu cầu cần xử lý tiếp với người phụ trách ban đầu.");
        Long id = ticket.getId();
        tickets.claimAsStaff(current.getUsername(), id);
        tickets.closeAsAdmin(current.getUsername(), id);

        tickets.reopenAsOwner("reopen-active-owner", id,
                "Vấn đề vẫn tồn tại nên cần được nhân viên trước xử lý.");
        assertThat(tickets.detailForOwner("reopen-active-owner", id)
                .getAssigneeUsername()).isEqualTo("reopen-active-staff");
        assertThat(tickets.assignmentHistory(id)).hasSize(1);
    }
}
