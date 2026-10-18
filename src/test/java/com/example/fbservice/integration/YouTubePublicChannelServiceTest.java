package com.example.fbservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class YouTubePublicChannelServiceTest {
    private static final String CHANNEL = "UC" + "x".repeat(22);

    private static void assertHttpStatus(Throwable failure, HttpStatus expected) {
        assertThat(failure).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) failure).getStatusCode()).isEqualTo(expected);
    }

    @Test
    void successfulReadIsOneReadOnlyOfficialApiCall() {
        AtomicInteger requests = new AtomicInteger();
        YouTubePublicChannelService subject = new YouTubePublicChannelService(
            "test-only-placeholder", new ObjectMapper(), request -> {
                requests.incrementAndGet();
                assertThat(request.method()).isEqualTo("GET");
                assertThat(request.uri().getScheme()).isEqualTo("https");
                assertThat(request.uri().getHost()).isEqualTo("www.googleapis.com");
                return new YouTubePublicChannelService.ProviderResponse(200,
                    """
                    {"items":[{"snippet":{"title":"Sample channel"},
                    "statistics":{"subscriberCount":"42","videoCount":"12",
                    "viewCount":"900","hiddenSubscriberCount":false}}]}
                    """);
            });
        Map<String, Object> summary = subject.channelSummary(CHANNEL);
        assertThat(summary).containsEntry("provider", "YOUTUBE")
                .containsEntry("channelId", CHANNEL)
                .containsEntry("title", "Sample channel")
                .containsEntry("subscriberCount", "42")
                .containsEntry("subscribersHidden", false);
        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void invalidIdsAndNoKeyMakeZeroOutboundCalls() {
        AtomicInteger attempts = new AtomicInteger();
        YouTubePublicChannelService noKey = new YouTubePublicChannelService(
            "", new ObjectMapper(), request -> {
                attempts.incrementAndGet();
                throw new AssertionError("Unexpected outbound call");
            });
        assertThatThrownBy(() -> noKey.channelSummary("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> noKey.channelSummary(CHANNEL))
                .satisfies(ex -> assertHttpStatus(ex, HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(attempts.get()).isZero();
    }

    @Test
    void rateLimitAndUpstreamFailuresAreSanitizedAndNeverRetried() {
        for (int responseStatus : new int[] {403, 429, 500}) {
            AtomicInteger attempts = new AtomicInteger();
            YouTubePublicChannelService subject = new YouTubePublicChannelService(
                "test-only-placeholder", new ObjectMapper(), request -> {
                    attempts.incrementAndGet();
                    return new YouTubePublicChannelService.ProviderResponse(
                        responseStatus, "private upstream body should not leak");
                });
            HttpStatus expected = responseStatus == 429 ?
                    HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
            assertThatThrownBy(() -> subject.channelSummary(CHANNEL))
                .satisfies(ex -> {
                    assertHttpStatus(ex, expected);
                    assertThat(ex.getMessage()).doesNotContain("private upstream body")
                            .doesNotContain("test-only-placeholder");
                });
            assertThat(attempts.get()).isEqualTo(1);
        }
    }

    @Test
    void timeoutsAndMalformedBodiesAreHandledWithoutLeakingSecrets() {
        YouTubePublicChannelService timeout = new YouTubePublicChannelService(
            "test-only-placeholder", new ObjectMapper(),
            request -> { throw new HttpTimeoutException("provider URI with secret"); });
        assertThatThrownBy(() -> timeout.channelSummary(CHANNEL))
            .satisfies(ex -> {
                assertHttpStatus(ex, HttpStatus.GATEWAY_TIMEOUT);
                assertThat(ex.getMessage()).doesNotContain("provider URI");
            });

        YouTubePublicChannelService malformed = new YouTubePublicChannelService(
            "test-only-placeholder", new ObjectMapper(),
            request -> new YouTubePublicChannelService.ProviderResponse(200, "{not-json"));
        assertThatThrownBy(() -> malformed.channelSummary(CHANNEL))
            .satisfies(ex -> assertHttpStatus(ex, HttpStatus.BAD_GATEWAY));

        YouTubePublicChannelService missing = new YouTubePublicChannelService(
            "test-only-placeholder", new ObjectMapper(),
            request -> new YouTubePublicChannelService.ProviderResponse(200, "{\"items\":[]}"));
        assertThatThrownBy(() -> missing.channelSummary(CHANNEL))
            .satisfies(ex -> assertHttpStatus(ex, HttpStatus.NOT_FOUND));
    }
    @Test
    void googleQuotaExceeded403IsNotMistakenForAuthorizationFailure() {
        AtomicInteger attempts = new AtomicInteger();
        YouTubePublicChannelService exhausted = new YouTubePublicChannelService(
            "test-only-placeholder", new ObjectMapper(), request -> {
                attempts.incrementAndGet();
                return new YouTubePublicChannelService.ProviderResponse(403,
                    "{\"error\":{\"code\":403,\"errors\":[{\"reason\":\"quotaExceeded\"}]}}");
            });
        assertThatThrownBy(() -> exhausted.channelSummary(CHANNEL))
            .satisfies(ex -> assertHttpStatus(ex, HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(attempts.get()).isEqualTo(1);

        YouTubePublicChannelService genericForbidden = new YouTubePublicChannelService(
            "test-only-placeholder", new ObjectMapper(),
            request -> new YouTubePublicChannelService.ProviderResponse(403,
                "{\"error\":{\"code\":403,\"errors\":[{\"reason\":\"insufficientPermissions\"}]}}"));
        assertThatThrownBy(() -> genericForbidden.channelSummary(CHANNEL))
            .satisfies(ex -> assertHttpStatus(ex, HttpStatus.BAD_GATEWAY));
    }

}
