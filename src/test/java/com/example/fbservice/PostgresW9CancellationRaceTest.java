package com.example.fbservice;

import com.example.fbservice.campaign.*;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real PostgreSQL row-lock regression for simultaneous customer cancellation.
 * No synthetic production data and no mocks of the transaction boundary.
 */
@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
    "spring.datasource.username=growlab",
    "spring.datasource.password=testpass",
    "server.servlet.session.cookie.secure=false"
})
class PostgresW9CancellationRaceTest {
    @Autowired CampaignOrderService service;

    @Test
    void twoConcurrentCancellationsCommitOnlyOneTransitionAndAuditEntry() throws Exception {
        String owner = "w9-race-" + UUID.randomUUID().toString().substring(0, 14);
        var order = service.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.PROFILE_AUDIT,
            "https://www.youtube.com/@growlab-race",
            "Kiểm tra hai thao tác hủy cùng lúc không ghi trùng lịch sử."));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Callable<Boolean> cancel = () -> {
                ready.countDown();
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting for concurrent start");
                }
                try {
                    service.cancelForOwner(owner, order.getId());
                    return true;
                } catch (IllegalArgumentException expectedAlreadyFinal) {
                    return false;
                }
            };
            Future<Boolean> first = pool.submit(cancel);
            Future<Boolean> second = pool.submit(cancel);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            release.countDown();

            boolean a = first.get(20, TimeUnit.SECONDS);
            boolean b = second.get(20, TimeUnit.SECONDS);
            assertThat(a != b).as("Exactly one cancellation must be accepted").isTrue();
            assertThat(service.findMineById(owner, order.getId()).getStatus())
                .isEqualTo(CampaignStatus.CANCELLED);
            assertThat(service.historyForOwner(owner, order.getId()))
                .as("Initial PENDING_REVIEW event + one cancellation event").hasSize(2);
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }
}
