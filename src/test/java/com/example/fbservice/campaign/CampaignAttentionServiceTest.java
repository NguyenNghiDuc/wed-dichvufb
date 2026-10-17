package com.example.fbservice.campaign;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CampaignAttentionServiceTest {
    private final CampaignAttentionService service = new CampaignAttentionService();
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 18, 12, 0);

    private CampaignOrder fresh() {
        CampaignOrder order = new CampaignOrder("unit-owner", new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@example", "Thông tin kênh chính xác hợp lệ"), 149000L);
        ReflectionTestUtils.setField(order, "createdAt", now.minusHours(48));
        ReflectionTestUtils.setField(order, "updatedAt", now.minusHours(168));
        return order;
    }

    @Test void pendingReviewAfter48HoursShowsNonContractualReminder() {
        CampaignOrder order=fresh();
        assertThat(service.inspect(order, now).overdue()).isTrue();
        assertThat(service.inspect(order, now).shortLabel()).contains("Chờ duyệt");
        ReflectionTestUtils.setField(order, "createdAt", now.minusHours(47));
        assertThat(service.inspect(order, now).needsAttention()).isFalse();
    }

    @Test void inProgressAfter168HoursIsFlaggedButCompletedIsNot() {
        CampaignOrder order=fresh();
        order.changeStatus(CampaignStatus.APPROVED);
        order.changeStatus(CampaignStatus.IN_PROGRESS);
        assertThat(service.inspect(order, now).overdue()).isTrue();
        order.changeStatus(CampaignStatus.COMPLETED);
        assertThat(service.inspect(order, now).needsAttention()).isFalse();
    }

    @Test void rejectedCampaignShowsSupportButNotRefundPromise() {
        CampaignOrder order=fresh();
        order.changeStatus(CampaignStatus.REJECTED);
        assertThat(service.inspect(order, now).needsAttention()).isTrue();
        assertThat(service.inspect(order, now).overdue()).isFalse();
        assertThat(service.inspect(order, now).message()).doesNotContain("đã hoàn tiền");
    }
}
