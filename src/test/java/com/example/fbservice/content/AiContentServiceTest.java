package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class AiContentServiceTest {
    private final ObjectMapper json=new ObjectMapper();

    private AiContentRequest legacy() {
        return new AiContentRequest(SocialPlatform.TIKTOK,"Useful study tips","Vietnamese students");
    }

    private void expectStatus(HttpStatus status,AiContentService service,AiContentRequest input) {
        assertThatThrownBy(()->service.generate(input))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(err->assertThat(((ResponseStatusException)err).getStatusCode()).isEqualTo(status));
    }

    @Test
    void missingKeyFailsBeforeSendingAnyNetworkCall() {
        AtomicInteger calls=new AtomicInteger();
        var service=new AiContentService("","test-model",json,req->{
            calls.incrementAndGet();
            throw new IOException("Should never be called");
        });
        expectStatus(HttpStatus.SERVICE_UNAVAILABLE,service,legacy());
        assertThat(calls.get()).isZero();
    }

    @Test
    void allFormatsUseOfficialEndpointWithBoundedTokensAndNoRetries() {
        AtomicInteger calls=new AtomicInteger();
        var service=new AiContentService("local-test-key","test-model",json,request->{
            calls.incrementAndGet();
            assertThat(request.uri().toString()).isEqualTo("https://api.openai.com/v1/chat/completions");
            assertThat(request.headers().firstValue("Authorization")).hasValue("Bearer local-test-key");
            assertThat(request.timeout()).isPresent();
            return new AiContentService.ProviderResponse(200,
                "{\"choices\":[{\"message\":{\"content\":\"Helpful human-written draft\"}}]}");
        });
        for(var task:AiContentTask.values()) {
            SocialPlatform platform = task==AiContentTask.YOUTUBE_SHORTS
                ? SocialPlatform.YOUTUBE : SocialPlatform.TIKTOK;
            var input=new AiContentRequest(platform,"Study tips","College students",
                task,AiContentLanguage.EN,7,"Improve this existing draft");
            var result=service.generate(input);
            assertThat(result.get("task")).isEqualTo(task.name());
            assertThat(result.get("language")).isEqualTo("EN");
            assertThat(result.get("content")).isEqualTo("Helpful human-written draft");
        }
        assertThat(calls.get()).isEqualTo(AiContentTask.values().length);
        assertThat(service.generate(legacy()).get("task")).isEqualTo("CAPTION");
        assertThat(calls.get()).isEqualTo(AiContentTask.values().length+1);
    }

    @Test
    void streamResponseIsBoundedBeforeDecodingAnyLargePayload() throws Exception {
        byte[] small="{\"choices\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(AiContentService.readBounded(new java.io.ByteArrayInputStream(small)))
            .isEqualTo("{\"choices\":[]}");
        var payload=new java.io.ByteArrayInputStream(new byte[2_000_000]);
        assertThatThrownBy(()->AiContentService.readBounded(payload))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(error -> assertThat(((ResponseStatusException)error).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY));
        assertThat(payload.available()).isGreaterThan(1_000_000);
        var service=new AiContentService("test-key","test-model",json,
            request->new AiContentService.ProviderResponse(200,"x".repeat(200_001)));
        expectStatus(HttpStatus.BAD_GATEWAY,service,legacy());
    }

    @Test
    void providerRateLimitsUpstreamErrorsAndInvalidPayloadsFailClosed() {
        var throttled=new AiContentService("fake","test-model",json,
            req->new AiContentService.ProviderResponse(429,"private provider details"));
        expectStatus(HttpStatus.SERVICE_UNAVAILABLE,throttled,legacy());
        var denied=new AiContentService("fake","test-model",json,
            req->new AiContentService.ProviderResponse(401,"secret auth details"));
        expectStatus(HttpStatus.BAD_GATEWAY,denied,legacy());
        var forbidden=new AiContentService("fake","test-model",json,
            req->new AiContentService.ProviderResponse(403,"secret auth details"));
        expectStatus(HttpStatus.BAD_GATEWAY,forbidden,legacy());
        var upstream=new AiContentService("fake","test-model",json,
            req->new AiContentService.ProviderResponse(500,"private provider details"));
        expectStatus(HttpStatus.BAD_GATEWAY,upstream,legacy());
        var providerTimeout=new AiContentService("fake","test-model",json,
            req->new AiContentService.ProviderResponse(504,"private provider details"));
        expectStatus(HttpStatus.GATEWAY_TIMEOUT,providerTimeout,legacy());
        var malformed=new AiContentService("fake","test-model",json,
            req->new AiContentService.ProviderResponse(200,"broken-json"));
        expectStatus(HttpStatus.BAD_GATEWAY,malformed,legacy());
        var empty=new AiContentService("fake","test-model",json,
            req->new AiContentService.ProviderResponse(200,"{\"choices\":[]}"));
        expectStatus(HttpStatus.BAD_GATEWAY,empty,legacy());
    }

    @Test
    void networkTimeoutAndProviderErrorsNeverLeakSensitiveDetails() {
        var timed=new AiContentService("fake","test-model",json,req->{
            throw new HttpTimeoutException("SECRET-DO-NOT-LOG");
        });
        expectStatus(HttpStatus.GATEWAY_TIMEOUT,timed,legacy());
        var io=new AiContentService("fake","test-model",json,req->{
            throw new IOException("private key=SECRET-DO-NOT-LOG");
        });
        assertThatThrownBy(()->io.generate(legacy()))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(err->{
                var e=(ResponseStatusException)err;
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                assertThat(e.getReason()).doesNotContain("SECRET-DO-NOT-LOG");
            });
    }


    @Test
    void mismatchedPlatformAndOversizedInputsNeverReachProvider() {
        AtomicInteger calls=new AtomicInteger();
        var service=new AiContentService("test-secret-only","test-model",json,req->{
            calls.incrementAndGet();
            return new AiContentService.ProviderResponse(200,"{}");
        });
        expectStatus(HttpStatus.BAD_REQUEST,service,
            new AiContentRequest(SocialPlatform.FACEBOOK,"Video idea","Students",
                AiContentTask.TIKTOK_IDEAS,AiContentLanguage.VI,7,null));
        expectStatus(HttpStatus.BAD_REQUEST,service,
            new AiContentRequest(SocialPlatform.TIKTOK,"Video idea","Students",
                AiContentTask.YOUTUBE_SHORTS,AiContentLanguage.VI,7,null));
        expectStatus(HttpStatus.BAD_REQUEST,service,
            new AiContentRequest(SocialPlatform.TIKTOK,"x".repeat(151),"Students"));
        expectStatus(HttpStatus.BAD_REQUEST,service,
            new AiContentRequest(SocialPlatform.YOUTUBE,"Useful video","Students",
                AiContentTask.OPTIMIZE,AiContentLanguage.EN,7," "));
        expectStatus(HttpStatus.BAD_REQUEST,service,
            new AiContentRequest(SocialPlatform.YOUTUBE,"Useful video","Students",
                AiContentTask.OPTIMIZE,AiContentLanguage.EN,7,"x".repeat(3001)));
        assertThat(calls.get()).isZero();
    }

    @Test
    void scriptAudienceAbAndProofreadHaveBoundedProviderRequests() {
        var service = new AiContentService("test-key", "test-model", json, request ->
            new AiContentService.ProviderResponse(200,
                "{\\"choices\\":[{\\"message\\":{\\"content\\":\\"Safe suggestion\\"}}]}"));
        for (var task : new AiContentTask[]{AiContentTask.VIDEO_SCRIPT,
                AiContentTask.TARGET_AUDIENCE, AiContentTask.AB_VARIANTS, AiContentTask.PROOFREAD}) {
            var input = new AiContentRequest(SocialPlatform.TIKTOK, "Study habits",
                "College students", task, AiContentLanguage.VI, 7, "Original draft");
            assertThat(service.generate(input).get("task")).isEqualTo(task.name());
        }
        expectStatus(HttpStatus.BAD_REQUEST, service,
            new AiContentRequest(SocialPlatform.FACEBOOK, "Study habits", "Students",
                AiContentTask.VIDEO_SCRIPT, AiContentLanguage.VI, 7, null));
        expectStatus(HttpStatus.BAD_REQUEST, service,
            new AiContentRequest(SocialPlatform.TIKTOK, "Study habits", "Students",
                AiContentTask.PROOFREAD, AiContentLanguage.VI, 7, null));
    }

    @Test
    void invalidPlanDaysOrMissingOriginalTextAreRejectedWithoutNetworkCall() {
        AtomicInteger calls=new AtomicInteger();
        var service=new AiContentService("fake","test-model",json,req->{
            calls.incrementAndGet();
            return new AiContentService.ProviderResponse(200,"{}");
        });
        expectStatus(HttpStatus.BAD_REQUEST,service,
            new AiContentRequest(SocialPlatform.YOUTUBE,"Video ideas","Readers",
                AiContentTask.CONTENT_PLAN,AiContentLanguage.VI,31,null));
        expectStatus(HttpStatus.BAD_REQUEST,service,
            new AiContentRequest(SocialPlatform.YOUTUBE,"Video ideas","Readers",
                AiContentTask.OPTIMIZE,AiContentLanguage.VI,null,null));
        assertThat(calls.get()).isZero();
    }
}
