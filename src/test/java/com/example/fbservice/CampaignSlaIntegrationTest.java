package com.example.fbservice;

import com.example.fbservice.campaign.*;
import com.example.fbservice.notification.AccountNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties={"spring.datasource.url=jdbc:h2:mem:slatest;DB_CLOSE_DELAY=-1",
    "growth.sla.alerts-enabled=false"})
class CampaignSlaIntegrationTest {
    @Autowired CampaignOrderService service;
    @Autowired CampaignOrderRepository orders;
    @Autowired CampaignSlaAlertService alerts;
    @Autowired AccountNotificationRepository notifications;

    private CampaignOrder newOrder(String username) {
        return service.create(username,new CampaignOrderRequest(
            SocialPlatform.YOUTUBE, CampaignServiceType.PROFILE_AUDIT,
            "https://www.youtube.com/@samplechannel",
            "Cần đánh giá hồ sơ và nội dung cho một kênh giáo dục."));
    }

    @Test
    void pendingDeadlineAndOneTimeAlertArePersisted() {
        CampaignOrder order=newOrder("sla-one");
        assertThat(order.getDueAt()).isAfter(LocalDateTime.now(ZoneOffset.UTC));
        order.scheduleDeadline(LocalDateTime.now(ZoneOffset.UTC).minusHours(2));
        orders.saveAndFlush(order);

        long notices=notifications.countByOwnerUsernameAndReadAtIsNull("sla-one");
        int delivered=alerts.scanNow();
        assertThat(delivered).isGreaterThanOrEqualTo(1);
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("sla-one"))
            .isEqualTo(notices+1);
        assertThat(orders.findById(order.getId()).orElseThrow().getOverdueNotifiedAt())
            .isNotNull();
        assertThat(alerts.scanNow()).isZero();
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("sla-one"))
            .isEqualTo(notices+1);
    }

    @Test
    void approvalResetsDeadlineAndClosedOrderNeverAlerts() {
        CampaignOrder order=newOrder("sla-two");
        order.scheduleDeadline(LocalDateTime.now(ZoneOffset.UTC).minusDays(2));
        orders.saveAndFlush(order);
        CampaignOrder approved=service.updateStatusForAdmin(order.getId(),CampaignStatus.APPROVED,"admin-sla");
        assertThat(approved.getDueAt()).isAfter(LocalDateTime.now(ZoneOffset.UTC).plusHours(70));
        assertThat(approved.isOverdue()).isFalse();
        assertThat(approved.getOverdueNotifiedAt()).isNull();

        long count=notifications.countByOwnerUsernameAndReadAtIsNull("sla-two");
        approved.scheduleDeadline(LocalDateTime.now(ZoneOffset.UTC).minusHours(4));
        approved.changeStatus(CampaignStatus.IN_PROGRESS);
        approved.changeStatus(CampaignStatus.COMPLETED);
        orders.saveAndFlush(approved);
        assertThat(alerts.scanNow()).isZero();
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("sla-two")).isEqualTo(count);
    }
}
