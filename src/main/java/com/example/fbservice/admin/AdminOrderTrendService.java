package com.example.fbservice.admin;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fixed-window aggregates. Database groups by day first (<= 366 rows),
 * then Java rolls up the 12-week/month or 14-day presentation buckets.
 */
@Service
public class AdminOrderTrendService {
    public enum Period { DAY, WEEK, MONTH }
    public record Bucket(String label, LocalDate start, long created, long completed) {}
    public record Trend(Period period, List<Bucket> buckets, long maximum, long totalCreated) {}
    private record Counts(long created, long completed) {}
    private final JdbcTemplate jdbc;

    public AdminOrderTrendService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public Trend find(Period period) { return find(period, LocalDate.now()); }

    /** Pure date-bucket assembly, allowing stable boundary tests without system clocks. */
    @Transactional(readOnly = true)
    public Trend find(Period period, LocalDate today) {
        if (period == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid period");
        LocalDate last = bucketStart(period, today);
        int slots = period == Period.DAY ? 14 : 12;
        LocalDate first = offset(period, last, -(slots - 1));
        LocalDate endExclusive = offset(period, last, 1);
        List<Map.Entry<LocalDate, Counts>> days = jdbc.query(
            "select cast(created_at as date) as day_key, count(*) as created_count, " +
            "sum(case when status = 'COMPLETED' then 1 else 0 end) as completed_count " +
            "from campaign_orders where created_at >= ? and created_at < ? " +
            "group by cast(created_at as date) order by day_key",
            (rs, n) -> Map.entry(rs.getDate("day_key").toLocalDate(),
                new Counts(rs.getLong("created_count"), rs.getLong("completed_count"))),
            Timestamp.valueOf(first.atStartOfDay()), Timestamp.valueOf(endExclusive.atStartOfDay()));
        Map<LocalDate, long[]> buckets = new LinkedHashMap<>();
        for (int i = 0; i < slots; i++) buckets.put(offset(period, first, i), new long[2]);
        for (var entry : days) {
            long[] count = buckets.get(bucketStart(period, entry.getKey()));
            if (count != null) {
                count[0] += entry.getValue().created;
                count[1] += entry.getValue().completed;
            }
        }
        long max = 1, total = 0;
        List<Bucket> result = new ArrayList<>(slots);
        for (var entry : buckets.entrySet()) {
            long created = entry.getValue()[0];
            max = Math.max(max, created);
            total += created;
            LocalDate start = entry.getKey();
            String label = switch(period) {
                case DAY -> String.format("%02d/%02d", start.getDayOfMonth(), start.getMonthValue());
                case WEEK -> String.format("%02d/%02d", start.getDayOfMonth(), start.getMonthValue());
                case MONTH -> String.format("%02d/%d", start.getMonthValue(), start.getYear());
            };
            result.add(new Bucket(label, start, created, entry.getValue()[1]));
        }
        return new Trend(period, List.copyOf(result), max, total);
    }

    private static LocalDate bucketStart(Period period, LocalDate date) {
        return switch (period) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private static LocalDate offset(Period period, LocalDate date, int amount) {
        return switch (period) {
            case DAY -> date.plusDays(amount);
            case WEEK -> date.plusWeeks(amount);
            case MONTH -> date.plusMonths(amount);
        };
    }
}
