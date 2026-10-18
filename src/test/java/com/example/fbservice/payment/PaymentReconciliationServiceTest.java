package com.example.fbservice.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PaymentReconciliationServiceTest {
    @Test
    void includesRollingDayCountsWithoutCustomerIdentifiers() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Map<String, Object> row = new HashMap<>();
        row.put("approved_without_receipt", 2L);
        row.put("inconsistent_receipts", 1L);
        row.put("internal_receipts", 12L);
        row.put("receipts_last_24h", 4L);
        row.put("internal_vnd_last_24h", 150000L);
        when(jdbc.queryForMap(anyString())).thenReturn(row);

        Map<String, Object> result = new PaymentReconciliationService(jdbc).summary();

        assertEquals(4L, result.get("receiptsLast24Hours"));
        assertEquals(150000L, result.get("internalVndLast24Hours"));
        assertEquals(12L, result.get("internalReceipts"));
        assertEquals(2L, result.get("approvedWithoutReceipt"));
        assertEquals(1L, result.get("inconsistentReceipts"));
        assertEquals(true, result.get("requiresManualReview"));
        assertEquals(true, result.get("notFinancialReconciliation"));
        assertEquals("SIGNED_INTERNAL_TEST_EVENTS_ONLY", result.get("source"));
        assertTrue(result.containsKey("generatedAt"));
        assertEquals(9, result.size());
        verify(jdbc).queryForMap(anyString());
    }
    @Test
    void zeroAnomaliesDoNotRequireReview() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Map<String, Object> row = new HashMap<>();
        row.put("approved_without_receipt", 0L);
        row.put("inconsistent_receipts", 0L);
        row.put("internal_receipts", 0L);
        row.put("receipts_last_24h", 0L);
        row.put("internal_vnd_last_24h", 0L);
        when(jdbc.queryForMap(anyString())).thenReturn(row);
        Map<String, Object> result = new PaymentReconciliationService(jdbc).summary();
        assertEquals(false, result.get("requiresManualReview"));
        assertEquals(0L, result.get("internalVndLast24Hours"));
    }

    @Test
    void mismatchWithoutMissingReceiptStillNeedsReview() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Map<String, Object> row = new HashMap<>();
        row.put("approved_without_receipt", 0L);
        row.put("inconsistent_receipts", 3L);
        row.put("internal_receipts", 3L);
        row.put("receipts_last_24h", 3L);
        row.put("internal_vnd_last_24h", 3000L);
        when(jdbc.queryForMap(anyString())).thenReturn(row);
        Map<String, Object> result = new PaymentReconciliationService(jdbc).summary();
        assertEquals(true, result.get("requiresManualReview"));
        assertEquals(3L, result.get("inconsistentReceipts"));
    }

    @Test
    void missingReceiptAloneTriggersReview() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Map<String, Object> row = new HashMap<>();
        row.put("approved_without_receipt", 1L);
        row.put("inconsistent_receipts", 0L);
        row.put("internal_receipts", 0L);
        row.put("receipts_last_24h", 0L);
        row.put("internal_vnd_last_24h", 0L);
        when(jdbc.queryForMap(anyString())).thenReturn(row);
        Map<String, Object> result = new PaymentReconciliationService(jdbc).summary();
        assertEquals(true, result.get("requiresManualReview"));
        assertEquals(1L, result.get("approvedWithoutReceipt"));
    }

}
