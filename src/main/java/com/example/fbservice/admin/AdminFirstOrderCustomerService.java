package com.example.fbservice.admin;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** First-ever campaign order in recent window, not account signup attribution. */
@Service
public class AdminFirstOrderCustomerService {
    private final JdbcTemplate jdbc;
    public AdminFirstOrderCustomerService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Transactional(readOnly = true)
    public long lastThirtyDays() {
        LocalDateTime now=LocalDateTime.now();
        return countFirstOrders(now.minusDays(30),now.plusSeconds(1));
    }

    @Transactional(readOnly = true)
    long countFirstOrders(LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        Long result=jdbc.queryForObject("""
            select count(distinct c.owner_username)
            from campaign_orders c
            where c.created_at >= ? and c.created_at < ?
              and not exists (
                select 1 from campaign_orders prior
                where prior.owner_username = c.owner_username
                  and prior.created_at < ?
              )
            """,Long.class,Timestamp.valueOf(fromInclusive),
            Timestamp.valueOf(toExclusive),Timestamp.valueOf(fromInclusive));
        return result == null ? 0L : result;
    }
}
