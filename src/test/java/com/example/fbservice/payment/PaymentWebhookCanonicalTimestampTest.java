package com.example.fbservice.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.fbservice.notification.AccountNotificationRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

class PaymentWebhookCanonicalTimestampTest {
    @Test
    void leadingZeroTimestampIsRejectedBeforeDatabaseTransaction() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        DepositRequestRepository deposits = mock(DepositRequestRepository.class);
        PaymentReceiptRepository receipts = mock(PaymentReceiptRepository.class);
        AccountNotificationRepository notifications = mock(AccountNotificationRepository.class);
        PaymentWebhookService service = new PaymentWebhookService(
                "sandbox-test-secret-with-at-least-32-bytes",
                deposits, receipts, notifications, new ObjectMapper(), transactionManager);

        String nonCanonicalTimestamp = "0" + Instant.now().getEpochSecond();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.accept("{}".getBytes(StandardCharsets.UTF_8),
                        "0".repeat(64), nonCanonicalTimestamp));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(transactionManager, deposits, receipts, notifications);
    }
    @Test
    void staleTimestampIsRejectedBeforeDatabaseTransaction() {
        assertRejectedBeforeDatabase(Long.toString(Instant.now().minusSeconds(3600).getEpochSecond()));
    }

    @Test
    void futureTimestampIsRejectedBeforeDatabaseTransaction() {
        assertRejectedBeforeDatabase(Long.toString(Instant.now().plusSeconds(3600).getEpochSecond()));
    }

    @Test
    void missingTimestampIsRejectedBeforeDatabaseTransaction() {
        assertRejectedBeforeDatabase(null);
    }

    @Test
    void nonDecimalTimestampIsRejectedBeforeDatabaseTransaction() {
        assertRejectedBeforeDatabase("2026-10-18T00:00:00Z");
    }

    private void assertRejectedBeforeDatabase(String timestamp) {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        DepositRequestRepository deposits = mock(DepositRequestRepository.class);
        PaymentReceiptRepository receipts = mock(PaymentReceiptRepository.class);
        AccountNotificationRepository notifications = mock(AccountNotificationRepository.class);
        PaymentWebhookService service = new PaymentWebhookService(
                "sandbox-test-secret-with-at-least-32-bytes",
                deposits, receipts, notifications, new ObjectMapper(), transactionManager);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.accept("{}".getBytes(StandardCharsets.UTF_8),
                        "0".repeat(64), timestamp));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(transactionManager, deposits, receipts, notifications);
    }

}
