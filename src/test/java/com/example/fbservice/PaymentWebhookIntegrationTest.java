package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.notification.AccountNotificationRepository;
import com.example.fbservice.payment.PaymentReceiptRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:paymenttest;DB_CLOSE_DELAY=-1",
        "growth.payments.webhook-secret=0123456789abcdef0123456789abcdef"
})
class PaymentWebhookIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DepositRequestRepository deposits;
    @Autowired PaymentReceiptRepository receipts;
    @Autowired AccountNotificationRepository notifications;
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private String sign(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    private String now() {
        return Long.toString(Instant.now().getEpochSecond());
    }

    private String event(String eventId, long id, long amount, String status) {
        return "{\"eventId\":\"" + eventId + "\",\"depositRequestId\":" + id
                + ",\"amountVnd\":" + amount + ",\"status\":\"" + status + "\"}";
    }

    private DepositRequest newDeposit(String owner, long amount) {
        DepositRequest deposit = new DepositRequest();
        deposit.setOwnerUsername(owner);
        deposit.setAmount(amount);
        return deposits.save(deposit);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(
            String body, String timestamp) throws Exception {
        return post("/api/v1/payments/webhooks/internal")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GrowLab-Timestamp", timestamp)
                .header("X-GrowLab-Signature", sign(timestamp, body))
                .content(body);
    }

    @Test
    void signedSettlementIsRecordedExactlyOnceWithoutCreditingWallet() throws Exception {
        DepositRequest deposit = newDeposit("webhook-owner-a", 50000);
        String body = event("sandboxEventA123", deposit.getId(), 50000, "SETTLED");
        String timestamp = now();
        long beforeReceipts = receipts.count();
        long beforeNotifications = notifications.count();
        mvc.perform(request(body, timestamp)).andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(false));
        mvc.perform(request(body, timestamp)).andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));
        assertThat(receipts.count()).isEqualTo(beforeReceipts + 1);
        assertThat(notifications.count()).isEqualTo(beforeNotifications + 1);
        assertThat(deposits.findById(deposit.getId()).orElseThrow().getStatus()).isEqualTo("APPROVED");
        mvc.perform(request(event("sandboxEventB123", deposit.getId(), 50000, "SETTLED"), now()))
                .andExpect(status().isConflict());
        assertThat(receipts.count()).isEqualTo(beforeReceipts + 1);
    }

    @Test
    void rejectsMissingForgedExpiredAndFutureSignaturesWithoutMutation() throws Exception {
        DepositRequest deposit = newDeposit("webhook-owner-b", 70000);
        String body = event("sandboxEventC123", deposit.getId(), 70000, "SETTLED");
        String timestamp = now();
        mvc.perform(post("/api/v1/payments/webhooks/internal")
                        .contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("X-GrowLab-Signature", sign(timestamp, body)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/payments/webhooks/internal")
                        .contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("X-GrowLab-Timestamp", timestamp)
                        .header("X-GrowLab-Signature", "0".repeat(64)))
                .andExpect(status().isUnauthorized());
        mvc.perform(request(body, Long.toString(Instant.now().minusSeconds(301).getEpochSecond())))
                .andExpect(status().isUnauthorized());
        mvc.perform(request(body, Long.toString(Instant.now().plusSeconds(301).getEpochSecond())))
                .andExpect(status().isUnauthorized());
        mvc.perform(request(body, timestamp).content(body.replace("70000", "70001")))
                .andExpect(status().isUnauthorized());
        assertThat(deposits.findById(deposit.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void rejectsInvalidAmountStatusUnknownDepositAndCrossDepositEventIdReuse() throws Exception {
        DepositRequest first = newDeposit("webhook-owner-c", 80000);
        DepositRequest second = newDeposit("webhook-owner-d", 80000);
        mvc.perform(request(event("sandboxEventD123", first.getId(), 80001, "SETTLED"), now()))
                .andExpect(status().isConflict());
        mvc.perform(request(event("sandboxEventD123", first.getId(), 80000, "PENDING"), now()))
                .andExpect(status().isBadRequest());
        mvc.perform(request(event("sandboxEventD123", Long.MAX_VALUE, 80000, "SETTLED"), now()))
                .andExpect(status().isNotFound());
        mvc.perform(request(event("sandboxEventD123", first.getId(), 80000, "SETTLED"), now()))
                .andExpect(status().isOk());
        mvc.perform(request(event("sandboxEventD123", second.getId(), 80000, "SETTLED"), now()))
                .andExpect(status().isConflict());
        assertThat(deposits.findById(second.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void rejectsMalformedOversizedAndUnauthenticatedBody() throws Exception {
        String timestamp = now();
        mvc.perform(request("not-json", timestamp)).andExpect(status().isBadRequest());
        mvc.perform(request("x".repeat(4097), timestamp)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/payments/webhooks/internal")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(request("{\"eventId\":\"short\"}", timestamp))
                .andExpect(status().isBadRequest());
    }
}
