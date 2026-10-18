package com.example.fbservice;

import com.example.fbservice.support.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:supportTicketActivity27;DB_CLOSE_DELAY=-1")
class SupportTicketActivityIntegrationTest {
    @Autowired SupportTicketService tickets;

    @Test
    void repeatedCustomerRepliesKeepOpenTicketRecentlyActiveAndSorted() throws Exception {
        var first = tickets.create("activity-first-owner", "Kiểm tra sắp xếp",
                "Bài kiểm tra hỗ trợ khi khách hàng gửi thêm phản hồi.");
        Thread.sleep(25);
        var second = tickets.create("activity-first-owner", "Yêu cầu mới hơn",
                "Yêu cầu này được tạo sau để kiểm tra thứ tự.");
        assertThat(tickets.searchMine("activity-first-owner", 0, null, null)
                .getContent().get(0).getId()).isEqualTo(second.getId());

        LocalDateTime initial = tickets.detailForOwner("activity-first-owner",
                first.getId()).getUpdatedAt();
        Thread.sleep(25);
        tickets.replyAsOwner("activity-first-owner", first.getId(), "First reply");
        LocalDateTime afterFirst = tickets.detailForOwner("activity-first-owner",
                first.getId()).getUpdatedAt();
        assertThat(afterFirst).isAfter(initial);
        Thread.sleep(25);
        tickets.replyAsOwner("activity-first-owner", first.getId(), "Second reply");
        LocalDateTime afterSecond = tickets.detailForOwner("activity-first-owner",
                first.getId()).getUpdatedAt();
        assertThat(afterSecond).isAfter(afterFirst);
        assertThat(tickets.detailForOwner("activity-first-owner", first.getId())
                .getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(tickets.searchMine("activity-first-owner", 0, null, null)
                .getContent().get(0).getId()).isEqualTo(first.getId());
    }

    @Test
    void repeatedStaffRepliesKeepAnsweredTicketRecentlyActive() throws Exception {
        var ticket = tickets.create("activity-second-owner", "Hỗ trợ theo dõi",
                "Nhân viên gửi các phản hồi liên tiếp cho khách hàng.");
        tickets.replyAsAdmin("activity-operator", ticket.getId(), "Initial answer");
        LocalDateTime first = tickets.detailForAdmin(ticket.getId()).getUpdatedAt();
        Thread.sleep(25);
        tickets.replyAsAdmin("activity-operator", ticket.getId(), "Further answer");
        LocalDateTime second = tickets.detailForAdmin(ticket.getId()).getUpdatedAt();
        assertThat(second).isAfter(first);
        assertThat(tickets.detailForAdmin(ticket.getId()).getStatus())
            .isEqualTo(TicketStatus.ANSWERED);
        assertThat(tickets.messagesForAdmin(ticket.getId())).hasSize(2);
    }
}
