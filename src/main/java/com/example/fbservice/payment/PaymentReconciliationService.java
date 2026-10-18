package com.example.fbservice.payment;

import java.time.Instant;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only, internal-event integrity check. Counts are not bank settlements,
 * revenue, wallet balances, or proof of external payment.
 */
@Service
public class PaymentReconciliationService {
    private final JdbcTemplate jdbc;

    public PaymentReconciliationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        // One SQL statement gives the counters a common PostgreSQL statement
        // snapshot even if callbacks are concurrently completing.
        var counts = jdbc.queryForMap("""
                SELECT
                  (SELECT COUNT(*) FROM deposit_request d
                     LEFT JOIN payment_receipts r ON r.deposit_request_id = d.id
                     WHERE d.status = 'APPROVED' AND r.id IS NULL) AS approved_without_receipt,
                  (SELECT COUNT(*) FROM payment_receipts r
                     LEFT JOIN deposit_request d ON d.id = r.deposit_request_id
                     WHERE d.id IS NULL OR d.amount <> r.amount_vnd
                        OR d.status <> 'APPROVED') AS inconsistent_receipts,
                  (SELECT COUNT(*) FROM payment_receipts) AS internal_receipts,
                  (SELECT COUNT(*) FROM payment_receipts
                     WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '1 day') AS receipts_last_24h,
                  (SELECT COALESCE(SUM(amount_vnd), 0) FROM payment_receipts
                     WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '1 day') AS internal_vnd_last_24h
                """);
        long missing = ((Number) counts.get("approved_without_receipt")).longValue();
        long mismatch = ((Number) counts.get("inconsistent_receipts")).longValue();
        long total = ((Number) counts.get("internal_receipts")).longValue();
        long recentCount = ((Number) counts.get("receipts_last_24h")).longValue();
        long recentAmount = ((Number) counts.get("internal_vnd_last_24h")).longValue();
        return Map.of(
                "receiptsLast24Hours", recentCount,
                "internalVndLast24Hours", recentAmount,
                "source", "SIGNED_INTERNAL_TEST_EVENTS_ONLY",
                "notFinancialReconciliation", true,
                "internalReceipts", total,
                "approvedWithoutReceipt", missing,
                "inconsistentReceipts", mismatch,
                "requiresManualReview", missing > 0 || mismatch > 0,
                "generatedAt", Instant.now().toString());
    }
}
