package com.example.fbservice;

import com.example.fbservice.support.SupportTicketService;
import com.example.fbservice.support.TicketStatus;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
        "spring.datasource.username=growlab",
        "spring.datasource.password=testpass",
        "server.servlet.session.cookie.secure=false"
})
class PostgresSupportReopenConcurrencyTest {
    @Autowired SupportTicketService tickets;

    @Test
    void onlyOneSimultaneousReopenOfTheSameClosedTicketSucceeds() throws Exception {
        var ticket = tickets.create("pg-reopen-owner", "Lỗi hỗ trợ chưa xong",
                "Tôi cần bổ sung thông tin để tiếp tục xử lý yêu cầu.");
        Long id = ticket.getId();
        tickets.closeAsAdmin("pg-reopen-agent", id);
        assertThat(tickets.detailForOwner("pg-reopen-owner", id).getStatus())
                .isEqualTo(TicketStatus.CLOSED);

        var pool = Executors.newFixedThreadPool(2);
        var gate = new CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Integer> task = () -> {
                gate.await();
                try {
                    tickets.reopenAsOwner("pg-reopen-owner", id,
                            "Sau khi kiểm tra, lỗi vẫn chưa được giải quyết.");
                    return 1;
                } catch (IllegalArgumentException alreadyOpen) {
                    return 0;
                }
            };
            Future<Integer> a = pool.submit(task);
            Future<Integer> b = pool.submit(task);
            gate.countDown();
            assertThat(a.get(20, TimeUnit.SECONDS) + b.get(20, TimeUnit.SECONDS))
                    .isEqualTo(1);
        } finally {
            gate.countDown();
            pool.shutdownNow();
        }
        assertThat(tickets.detailForOwner("pg-reopen-owner", id).getStatus())
                .isEqualTo(TicketStatus.OPEN);
        // Closed lifecycle entry + reopen entry + exactly one owner reason.
        assertThat(tickets.messagesForOwner("pg-reopen-owner", id)).hasSize(3);
    }
}
