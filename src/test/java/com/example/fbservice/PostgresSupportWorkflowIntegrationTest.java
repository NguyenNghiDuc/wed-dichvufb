package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.support.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties={
    "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
    "spring.datasource.username=growlab",
    "spring.datasource.password=testpass",
    "server.servlet.session.cookie.secure=false"
})
class PostgresSupportWorkflowIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired SupportTicketService tickets;

    @Test
    void flywayCreatesTicketAuditAndPagingIndexesOnRealPostgres() {
        Integer indexCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname = current_schema()
                  AND indexname IN ('idx_support_msg_timeline',
                                    'idx_support_staff_queue',
                                    'idx_support_assignee',
                                    'idx_support_assignment_ticket',
                                    'idx_support_owner_queue',
                                    'idx_chat_received_unread')
                """, Integer.class);
        assertThat(indexCount).isEqualTo(6);

        User operator = new User();
        operator.setUsername("pg-support-reassign-admin");
        operator.setPassword("test-only-not-real");
        operator.setRole("ADMIN");
        users.save(operator);
        User staff = new User();
        staff.setUsername("pg-support-reassign-staff");
        staff.setPassword("test-only-not-real");
        staff.setRole("STAFF");
        staff=users.save(staff);

        SupportTicket ticket = tickets.create("pg-support-ticket-owner",
            "Phân trang và kiểm thử thực tế",
            "Yêu cầu được kiểm tra trực tiếp trên PostgreSQL.");
        tickets.reassignAsAdmin(operator.getUsername(), ticket.getId(), staff.getId());
        assertThat(tickets.assignmentHistory(ticket.getId())).hasSize(1);
        assertThat(tickets.detailForOwner("pg-support-ticket-owner", ticket.getId())
            .getAssigneeUsername()).isEqualTo(staff.getUsername());
        assertThat(tickets.searchMine("pg-support-ticket-owner", 0, "kiểm thử", null)
            .getContent()).anyMatch(t -> t.getId().equals(ticket.getId()));
        assertThat(tickets.searchMine("pg-support-foreign", 0, "kiểm thử", null)
            .getContent()).isEmpty();
    }
}
