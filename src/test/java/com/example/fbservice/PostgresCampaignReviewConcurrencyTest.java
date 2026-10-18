package com.example.fbservice;

import com.example.fbservice.campaign.*;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties={
    "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
    "spring.datasource.username=growlab",
    "spring.datasource.password=testpass",
    "server.servlet.session.cookie.secure=false"
})
class PostgresCampaignReviewConcurrencyTest {
    @Autowired CampaignOrderService orders;
    @Autowired CampaignReviewService reviews;
    @Autowired JdbcTemplate jdbc;

    @Test
    void twoSimultaneousSubmissionsLeaveExactlyOneOwnerReview() throws Exception {
        String owner = "pg-review-race-issue26";
        CampaignOrder order = orders.create(owner, new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@validchannel",
            "Kế hoạch nội dung có giá trị phù hợp chính sách."));
        orders.updateStatusForAdmin(order.getId(), CampaignStatus.APPROVED, "staff");
        orders.updateStatusForAdmin(order.getId(), CampaignStatus.IN_PROGRESS, "staff");
        orders.updateStatusForAdmin(order.getId(), CampaignStatus.COMPLETED, "staff");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<String> request = () -> {
            ready.countDown();
            if (!start.await(15, TimeUnit.SECONDS))
                throw new IllegalStateException("test start timed out");
            try {
                reviews.submit(owner, order.getId(),
                    new CampaignReviewInput(5, "Đánh giá hợp lệ và hữu ích cho dịch vụ."));
                return "CREATED";
            } catch (ResponseStatusException e) {
                if (e.getStatusCode() != HttpStatus.CONFLICT) throw e;
                return "CONFLICT";
            }
        };
        try {
            Future<String> first = executor.submit(request);
            Future<String> second = executor.submit(request);
            assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS),
                second.get(20, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder("CREATED", "CONFLICT");
            assertThat(jdbc.queryForObject(
                "select count(*) from campaign_reviews where campaign_id=?",
                Long.class, order.getId())).isEqualTo(1L);
            assertThat(reviews.findForOwner(owner, order.getId())).isPresent();
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
