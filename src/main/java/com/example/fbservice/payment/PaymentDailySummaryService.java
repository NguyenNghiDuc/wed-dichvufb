package com.example.fbservice.payment;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Internal sandbox receipt counts, not bank settlement or actual revenue.
 * Uses the server's configured local timezone because the legacy receipt
 * created_at column is a TIMESTAMP WITHOUT TIME ZONE.
 */
@Service
public class PaymentDailySummaryService {
    public static final int MAX_DAYS = 31;
    private final JdbcTemplate jdbc;

    public PaymentDailySummaryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record DaySummary(LocalDate date, long internalReceiptCount, long internalAmountVnd) {}

    @Transactional(readOnly = true)
    public Map<String, Object> report(int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be 1 to 31");
        }
        LocalDate today = LocalDate.now();
        LocalDate first = today.minusDays(days - 1L);
        LocalDateTime start = first.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        // Single bounded indexed scan; fill missing days with zeros so that
        // callers do not mistake an absent bucket for a failed query.
        var observed = jdbc.query("""
                SELECT CAST(created_at AS DATE) AS receipt_day,
                       COUNT(*) AS receipt_count,
                       COALESCE(SUM(amount_vnd), 0) AS internal_amount
                FROM payment_receipts
                WHERE created_at >= ? AND created_at < ?
                GROUP BY CAST(created_at AS DATE)
                ORDER BY receipt_day DESC
                """, ps -> {
                    ps.setTimestamp(1, Timestamp.valueOf(start));
                    ps.setTimestamp(2, Timestamp.valueOf(end));
                }, (rs, rowNum) -> new DaySummary(
                        rs.getDate("receipt_day").toLocalDate(),
                        rs.getLong("receipt_count"),
                        new BigDecimal(rs.getString("internal_amount")).longValueExact()));
        Map<LocalDate, DaySummary> byDay = new HashMap<>();
        for (DaySummary entry : observed) {
            byDay.put(entry.date(), entry);
        }
        List<DaySummary> daily = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate day = today.minusDays(i);
            daily.add(byDay.getOrDefault(day, new DaySummary(day, 0L, 0L)));
        }
        return Map.of(
                "source", "SIGNED_INTERNAL_TEST_EVENTS_ONLY",
                "notFinancialReconciliation", true,
                "timeZone", ZoneId.systemDefault().getId(),
                "days", days,
                "daily", daily);
    }
}
