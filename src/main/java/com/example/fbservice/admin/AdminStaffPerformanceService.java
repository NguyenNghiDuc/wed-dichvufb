package com.example.fbservice.admin;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Event-based operational counts, not employee ratings or payroll metrics. */
@Service
public class AdminStaffPerformanceService {
    public record StaffPerformance(String actor, long started, long completed) {}
    private final JdbcTemplate jdbc;
    public AdminStaffPerformanceService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Transactional(readOnly=true)
    public List<StaffPerformance> lastThirtyDays() {
        return since(LocalDateTime.now().minusDays(30));
    }

    @Transactional(readOnly=true)
    List<StaffPerformance> since(LocalDateTime since) {
        return jdbc.query("""
            select actor,
                sum(case when new_status='IN_PROGRESS' then 1 else 0 end) as started_count,
                sum(case when new_status='COMPLETED' then 1 else 0 end) as completed_count
            from campaign_events
            where description='Nhân viên phụ trách cập nhật tiến độ'
              and created_at >= ?
            group by actor
            order by completed_count desc, started_count desc, actor asc
            limit 20
            """, (rs, i) -> new StaffPerformance(rs.getString("actor"),
                rs.getLong("started_count"), rs.getLong("completed_count")),
            Timestamp.valueOf(since));
    }

    @Transactional(readOnly = true)
    public List<StaffPerformance> filtered(String actor) {
        if (actor == null || actor.isBlank()) return lastThirtyDays();
        String exact = actor.trim();
        if (!exact.matches("[a-zA-Z0-9._-]{3,100}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid staff actor");
        return List.of(forActor(exact));
    }

    @Transactional(readOnly=true)
    public StaffPerformance forActor(String username) {
        if (username == null || username.isBlank())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var rows = jdbc.query("""
            select
                sum(case when new_status='IN_PROGRESS' then 1 else 0 end) as started_count,
                sum(case when new_status='COMPLETED' then 1 else 0 end) as completed_count
            from campaign_events
            where description='Nhân viên phụ trách cập nhật tiến độ'
              and actor=? and created_at >= ?
            """, (rs, i) -> new StaffPerformance(username,
                rs.getLong("started_count"), rs.getLong("completed_count")),
            username, Timestamp.valueOf(LocalDateTime.now().minusDays(30)));
        return rows.isEmpty() ? new StaffPerformance(username,0,0) : rows.get(0);
    }
}
