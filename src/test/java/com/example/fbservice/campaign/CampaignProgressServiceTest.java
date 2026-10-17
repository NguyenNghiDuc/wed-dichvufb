package com.example.fbservice.campaign;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CampaignProgressServiceTest {
    private final CampaignProgressService progress = new CampaignProgressService();

    CampaignOrder order() {
        return new CampaignOrder("progress-user", new CampaignOrderRequest(
            SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,
            "https://youtube.com/@channel", "Tối ưu hóa nội dung thật của kênh"),149000);
    }

    @Test void progressStagesRespectLifecycleAndTerminalStates() {
        CampaignOrder o=order();
        assertThat(progress.view(o).percent()).isZero();
        assertThat(progress.view(o).steps().get(0).active()).isTrue();
        o.changeStatus(CampaignStatus.APPROVED);
        assertThat(progress.view(o).percent()).isEqualTo(33);
        o.changeStatus(CampaignStatus.IN_PROGRESS);
        assertThat(progress.view(o).percent()).isEqualTo(67);
        o.changeStatus(CampaignStatus.COMPLETED);
        assertThat(progress.view(o).percent()).isEqualTo(100);
        assertThat(progress.view(o).steps().get(3).active()).isTrue();
        assertThat(progress.view(o).closedWithoutCompletion()).isFalse();
    }

    @Test void cancelledOrRejectedDoesNotLookCompleted() {
        CampaignOrder cancelled=order();
        cancelled.changeStatus(CampaignStatus.CANCELLED);
        assertThat(progress.view(cancelled).closedWithoutCompletion()).isTrue();
        assertThat(progress.view(cancelled).percent()).isZero();
        assertThat(progress.view(cancelled).steps()).allSatisfy(step ->
            assertThat(step.active() || step.done()).isFalse());
        CampaignOrder rejected=order();
        rejected.changeStatus(CampaignStatus.REJECTED);
        assertThat(progress.view(rejected).closedWithoutCompletion()).isTrue();
    }
}
