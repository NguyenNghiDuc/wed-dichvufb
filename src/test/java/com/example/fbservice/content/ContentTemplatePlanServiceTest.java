package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ContentTemplatePlanServiceTest {
    private final ContentTemplatePlanService templates=new ContentTemplatePlanService();

    @Test
    void generatesExactlySevenToThirtyExplicitlyNonAiItems() {
        for(int days:new int[]{7,14,30}) {
            var result=templates.plan(SocialPlatform.TIKTOK,"Content education",days);
            assertThat(result.mode()).isEqualTo("TEMPLATE_ONLY");
            assertThat(result.days()).isEqualTo(days);
            assertThat(result.suggestions()).hasSize(days);
            assertThat(result.suggestions().get(0).day()).isEqualTo(1);
            assertThat(result.suggestions().get(days-1).day()).isEqualTo(days);
            assertThat(result.notice()).contains("KHÔNG phải AI").contains("KHÔNG tự");
        }
    }

    @Test
    void invalidTopicAndDayBoundariesCannotGeneratePlan() {
        assertThatThrownBy(()->templates.plan(null,"Topic",7))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->templates.plan(SocialPlatform.YOUTUBE," ",7))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->templates.plan(SocialPlatform.YOUTUBE,"Good topic",6))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->templates.plan(SocialPlatform.YOUTUBE,"Good topic",31))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->templates.plan(SocialPlatform.YOUTUBE,"Bad\nTopic",7))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
