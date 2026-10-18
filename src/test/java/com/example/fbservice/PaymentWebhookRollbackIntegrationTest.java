package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.notification.AccountNotification;
import com.example.fbservice.notification.AccountNotificationRepository;
import com.example.fbservice.payment.PaymentReceiptRepository;
import com.example.fbservice.payment.PaymentWebhookService;
import com.example.fbservice.repository.DepositRequestRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:paymentrollback;DB_CLOSE_DELAY=-1",
        "growth.payments.webhook-secret=unit-only-rollback-hmac-secret-0123456789"
})
class PaymentWebhookRollbackIntegrationTest {
    private static final String KEY = "unit-only-rollback-hmac-secret-0123456789";

    @Autowired PaymentWebhookService webhook;
    @Autowired DepositRequestRepository deposits;
    @Autowired PaymentReceiptRepository receipts;
    @MockBean AccountNotificationRepository notifications;

    @Test
    void notificationWriteFailureRollsBackReceiptAndDepositState() throws Exception {
        DepositRequest deposit = new DepositRequest();
        deposit.setOwnerUsername("payment-rollback-owner");
        deposit.setAmount(73000);
        deposit = deposits.saveAndFlush(deposit);
        Long depositId = deposit.getId();
        String eventId = "rollbackEventW6Internal";
        String body = "{\"eventId\":\"" + eventId + "\",\"depositRequestId\":"
                + depositId + ",\"amountVnd\":73000,\"status\":\"SETTLED\"}";
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));

        doThrow(new IllegalStateException("simulated internal notification storage outage"))
                .when(notifications).save(any(AccountNotification.class));

        assertThatThrownBy(() -> webhook.accept(
                body.getBytes(StandardCharsets.UTF_8), signature, timestamp))
                .isInstanceOf(IllegalStateException.class);
        verify(notifications, times(1)).save(any(AccountNotification.class));
        assertThat(receipts.findByEventId(eventId)).isEmpty();
        assertThat(deposits.findById(depositId).orElseThrow().getStatus())
                .isEqualTo("PENDING");
    }
}
