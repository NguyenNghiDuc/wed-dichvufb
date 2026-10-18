package com.example.fbservice;

import com.example.fbservice.campaign.*;
import java.util.List;
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
class PostgresCustomerCancellationRaceTest {
    @Autowired CampaignOrderService orders;

    @Test
    void customerCancelAndAdminApprovalCannotBothWin() throws Exception {
        String owner = "pg-cancel-race-owner";
        CampaignOrder order = orders.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@channel",
            "Lịch nội dung hữu ích cho tài khoản thật."));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<String> cancel = () -> {
            ready.countDown();
            if (!start.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
            try {
                orders.cancelForOwner(owner, order.getId());
                return "CANCELLED";
            } catch (IllegalArgumentException e) {
                return "CANCEL_DENIED";
            }
        };
        Callable<String> approve = () -> {
            ready.countDown();
            if (!start.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
            try {
                orders.updateStatusForAdmin(order.getId(), CampaignStatus.APPROVED, "reviewer");
                return "APPROVED";
            } catch (IllegalArgumentException e) {
                return "APPROVE_DENIED";
            }
        };
        try {
            Future<String> a = executor.submit(cancel);
            Future<String> b = executor.submit(approve);
            assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<String> results = List.of(a.get(20, TimeUnit.SECONDS),
                b.get(20, TimeUnit.SECONDS));
            assertThat(results.contains("CANCELLED") ^ results.contains("APPROVED")).isTrue();
            if (results.contains("CANCELLED")) {
                assertThat(results).containsExactlyInAnyOrder("CANCELLED", "APPROVE_DENIED");
                assertThat(orders.findMineById(owner, order.getId()).getStatus())
                    .isEqualTo(CampaignStatus.CANCELLED);
            } else {
                assertThat(results).containsExactlyInAnyOrder("CANCEL_DENIED", "APPROVED");
                assertThat(orders.findMineById(owner, order.getId()).getStatus())
                    .isEqualTo(CampaignStatus.APPROVED);
            }
            assertThat(orders.historyForOwner(owner, order.getId())).hasSize(2);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
