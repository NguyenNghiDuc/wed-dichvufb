package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class ContentMetricsServiceTest {
    private final ContentMetricsService metrics=new ContentMetricsService();

    private ContentMetricsRequest.Metric sample(SocialPlatform platform,int hour,int likes) {
        LocalDateTime past=LocalDateTime.now(ZoneOffset.UTC).minusDays(30)
            .withHour(hour).withMinute(0).withSecond(0).withNano(0);
        return new ContentMetricsRequest.Metric(platform,past,100,likes,0,0,0);
    }

    @Test
    void aggregatesOnlyProvidedValuesWithExplicitUnverifiedProvenance() {
        var result=metrics.analyze(new ContentMetricsRequest(List.of(
            sample(SocialPlatform.TIKTOK,8,15),
            sample(SocialPlatform.TIKTOK,12,5)
        )));
        assertThat(result.source()).isEqualTo("USER_SUPPLIED_UNVERIFIED");
        assertThat(result.postCount()).isEqualTo(2);
        assertThat(result.totalViews()).isEqualTo(200);
        assertThat(result.totalInteractions()).isEqualTo(20);
        assertThat(result.interactionsPer100Views().toPlainString()).isEqualTo("10.00");
        assertThat(result.candidateHourUtc()).isNull();
        assertThat(result.supportingPosts()).isNull();
        assertThat(result.caveat()).contains("chưa được nền tảng xác minh");
    }

    @Test
    void suggestsUtcHourOnlyForSufficientSamePlatformHistoricalSamples() {
        List<ContentMetricsRequest.Metric> observations=new ArrayList<>();
        for(int i=0;i<4;i++) {
            observations.add(sample(SocialPlatform.TIKTOK,8,20));
            observations.add(sample(SocialPlatform.TIKTOK,12,10));
            observations.add(sample(SocialPlatform.TIKTOK,19,5));
        }
        var result=metrics.analyze(new ContentMetricsRequest(observations));
        assertThat(result.candidateHourUtc()).isEqualTo(8);
        assertThat(result.supportingPosts()).isEqualTo(4);
        assertThat(result.totalViews()).isEqualTo(1200);
        assertThat(result.interactionsPer100Views().toPlainString()).isEqualTo("11.67");
        // Mixing platforms suppresses misleading cross-platform posting-time comparisons.
        observations.set(0,sample(SocialPlatform.YOUTUBE,8,20));
        assertThat(metrics.analyze(new ContentMetricsRequest(observations))
            .candidateHourUtc()).isNull();
    }

    @Test
    void invalidInputsNeverProduceStatistics() {
        assertBad(new ContentMetricsRequest(List.of()));
        assertBad(new ContentMetricsRequest(null));
        List<ContentMetricsRequest.Metric> excessive=new ArrayList<>();
        for(int i=0;i<101;i++)excessive.add(sample(SocialPlatform.FACEBOOK,10,1));
        assertBad(new ContentMetricsRequest(excessive));
        assertBad(new ContentMetricsRequest(List.of(
            new ContentMetricsRequest.Metric(SocialPlatform.YOUTUBE,
                LocalDateTime.now(ZoneOffset.UTC).plusDays(1),10,1,0,0,0))));
        assertBad(new ContentMetricsRequest(List.of(
            new ContentMetricsRequest.Metric(SocialPlatform.YOUTUBE,
                LocalDateTime.now(ZoneOffset.UTC).minusYears(3),10,1,0,0,0))));
        assertBad(new ContentMetricsRequest(List.of(
            new ContentMetricsRequest.Metric(SocialPlatform.YOUTUBE,
                LocalDateTime.now(ZoneOffset.UTC).minusDays(2),10,-1,0,0,0))));
    }

    private void assertBad(ContentMetricsRequest input) {
        assertThatThrownBy(()->metrics.analyze(input))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
