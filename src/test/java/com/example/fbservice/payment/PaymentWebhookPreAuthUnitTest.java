package com.example.fbservice.payment;

import com.example.fbservice.notification.AccountNotificationRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PaymentWebhookPreAuthUnitTest {
    private static final String TEST_SECRET = "test-only-unit-signature-key-0123456789";

    private final DepositRequestRepository deposits = mock(DepositRequestRepository.class);
    private final PaymentReceiptRepository receipts = mock(PaymentReceiptRepository.class);
    private final AccountNotificationRepository notifications = mock(AccountNotificationRepository.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final PaymentWebhookService service = new PaymentWebhookService(
            TEST_SECRET, deposits, receipts, notifications, new ObjectMapper(), manager);

    private String sign(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void forgedSignatureDoesNotOpenTransactionOrTouchRepositories() {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        assertThatThrownBy(() -> service.accept("{}".getBytes(StandardCharsets.UTF_8),
                "0".repeat(64), timestamp))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(manager, deposits, receipts, notifications);
    }

    @Test
    void authenticatedButMalformedJsonDoesNotOpenTransaction() throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String invalidJson = "{not json}";
        assertThatThrownBy(() -> service.accept(invalidJson.getBytes(StandardCharsets.UTF_8),
                sign(timestamp, invalidJson), timestamp))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(manager, deposits, receipts, notifications);
    }
}
