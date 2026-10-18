package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.support.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class PostgresSupportConcurrencyIntegrationTest {
    @Autowired UserRepository users;
    @Autowired SupportTicketService tickets;

    private void activeStaff(String name) {
        User account = new User();
        account.setUsername(name);
        account.setPassword("test-only-not-real");
        account.setEnabled(true);
        account.setRole("STAFF");
        users.save(account);
    }

    @Test
    void concurrentClaimsMustProduceExactlyOneOwnerAndOneAuditEntry() throws Exception {
        activeStaff("pg-race-worker-one");
        activeStaff("pg-race-worker-two");
        SupportTicket ticket = tickets.create("pg-race-customer",
            "Tiếp nhận trong điều kiện đồng thời",
            "Hai nhân viên cùng tiếp nhận một yêu cầu hỗ trợ.");
        Long id = ticket.getId();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = pool.submit(() -> claimAfter(start, "pg-race-worker-one", id));
            Future<Boolean> second = pool.submit(() -> claimAfter(start, "pg-race-worker-two", id));
            start.countDown();
            int successful = (first.get(30, TimeUnit.SECONDS) ? 1 : 0)
                           + (second.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertThat(successful).isEqualTo(1);
            assertThat(tickets.assignmentHistory(id)).hasSize(1);
            assertThat(tickets.detailForAdmin(id).getAssigneeUsername())
                .isIn("pg-race-worker-one", "pg-race-worker-two");
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean claimAfter(CountDownLatch start, String user, Long ticketId)
            throws InterruptedException {
        if (!start.await(15, TimeUnit.SECONDS))
            throw new IllegalStateException("Concurrent test start timed out");
        try {
            tickets.claimAsStaff(user, ticketId);
            return true;
        } catch (IllegalArgumentException expectedConflict) {
            return false;
        }
    }
}
