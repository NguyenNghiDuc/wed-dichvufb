package com.example.fbservice.admin;

import com.example.fbservice.campaign.CampaignAssignmentEvent;
import com.example.fbservice.campaign.CampaignEvent;
import com.example.fbservice.catalog.ServiceCatalogAudit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only, bounded view of four existing audit/event sources.
 * This is a convenience timeline, not a tamper-proof audit system.
 */
@Service
public class AdminActivityService {
    public enum Area { ALL, ACCOUNTS, CATALOG, ASSIGNMENTS, ORDER_STATUS, STAFF_STATUS }

    public record Entry(Area area, String actor, String target, String details,
                        LocalDateTime createdAt, long sourceId) {}

    private static final int LIMIT_PER_SOURCE = 50;
    private static final int DISPLAY_LIMIT = 50;
    private final EntityManager entityManager;

    public AdminActivityService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<Entry> recent(Area area) {
        return recent(area, null);
    }

    /**
     * Actor filter runs in the database before the per-source 50-row cap.
     * No user-controlled JPQL, source names or sort columns.
     */
    @Transactional(readOnly = true)
    public List<Entry> recent(Area area, String actor) {
        return recent(area, actor, null, null);
    }

    /** Inclusive dates, before the per-source limits, in server-local time. */
    @Transactional(readOnly = true)
    public List<Entry> recent(Area area, String actor, LocalDate from, LocalDate to) {
        if (area == null) throw new IllegalArgumentException("Invalid activity category");
        if ((from != null && (from.getYear() < 2000 || from.getYear() > 2100)) ||
            (to != null && (to.getYear() < 2000 || to.getYear() > 2100)) ||
            (from != null && to != null && from.isAfter(to)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid audit date range");
        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
        String exactActor = actor == null || actor.isBlank() ? null : actor.trim();
        if (exactActor != null && !exactActor.matches("[a-zA-Z0-9._-]{3,100}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid audit actor");
        List<Entry> entries = new ArrayList<>();
        if (area == Area.ALL || area == Area.ACCOUNTS) {
            readSource(AdminAccountAudit.class, exactActor, start, end).forEach(e ->
                    entries.add(new Entry(Area.ACCOUNTS, e.getActor(),
                        "Tài khoản #" + e.getTargetUserId(),
                        e.getOperation() + ": " + e.getBeforeValue() + " → " + e.getAfterValue(),
                        e.getCreatedAt(), e.getId())));
        }
        if (area == Area.ALL || area == Area.CATALOG) {
            readSource(ServiceCatalogAudit.class, exactActor, start, end).forEach(e ->
                    entries.add(new Entry(Area.CATALOG, e.getActor(), "Dịch vụ " + e.getServiceCode(),
                        "Báo giá: " + e.getOldPrice() + " → " + e.getNewPrice() + " VND; mở bán: " +
                            e.isOldEnabled() + " → " + e.isNewEnabled(),
                        e.getCreatedAt(), e.getId())));
        }
        if (area == Area.ALL || area == Area.ASSIGNMENTS) {
            readSource(CampaignAssignmentEvent.class, exactActor, start, end).forEach(e ->
                    entries.add(new Entry(Area.ASSIGNMENTS, e.getActor(),
                        "Chiến dịch #" + e.getCampaignId(),
                        "Phân công: " + (e.getOldAssignee() == null ? "Chưa giao" : e.getOldAssignee()) +
                        " → " + (e.getNewAssignee() == null ? "Chưa giao" : e.getNewAssignee()),
                        e.getCreatedAt(), e.getId())));
        }
        if (area == Area.ALL || area == Area.ORDER_STATUS) {
            // Customer-initiated and staff-initiated campaign events are not Admin actions.
            readOrderStatusSource(exactActor, "Quản trị viên cập nhật trạng thái", start, end).forEach(e ->
                    entries.add(new Entry(Area.ORDER_STATUS, e.getActor(),
                        "Chiến dịch #" + e.getCampaignId(),
                        "Trạng thái: " + (e.getOldStatus() == null ? "—" : e.getOldStatus().name()) +
                            " → " + e.getNewStatus().name(),
                        e.getCreatedAt(), e.getId())));
        }
        if (area == Area.ALL || area == Area.STAFF_STATUS) {
            // STAFF actions are recorded by the existing assignment/progress service.
            // Do not conflate customer-request events with staff audit actions.
            readOrderStatusSource(exactActor, "Nhân viên phụ trách cập nhật tiến độ", start, end).forEach(e ->
                entries.add(new Entry(Area.STAFF_STATUS, e.getActor(),
                    "Chiến dịch #" + e.getCampaignId(),
                    "Tiến độ: " + (e.getOldStatus() == null ? "—" : e.getOldStatus().name()) +
                        " → " + e.getNewStatus().name(),
                    e.getCreatedAt(), e.getId())));
        }
        return entries.stream()
            .sorted(Comparator.comparing(Entry::createdAt).reversed()
                .thenComparing(Entry::area)
                .thenComparing(Comparator.comparingLong(Entry::sourceId).reversed()))
            .limit(DISPLAY_LIMIT).toList();
    }

    private <T> List<T> readSource(Class<T> type, String actor,
                                   LocalDateTime start, LocalDateTime end) {
        // Only our fixed entity classes, fields and sort order ever become query text.
        String query = "select e from " + type.getSimpleName() + " e where 1=1" +
            (actor == null ? "" : " and e.actor = :actor") +
            (start == null ? "" : " and e.createdAt >= :start") +
            (end == null ? "" : " and e.createdAt < :end") +
            " order by e.createdAt desc, e.id desc";
        TypedQuery<T> typed = entityManager.createQuery(query, type);
        bindFilters(typed, actor, start, end);
        return typed.setMaxResults(LIMIT_PER_SOURCE).getResultList();
    }

    private List<CampaignEvent> readOrderStatusSource(String actor, String description,
                                                       LocalDateTime start, LocalDateTime end) {
        String query = "select e from CampaignEvent e " +
            "where e.description = :description" +
            (actor == null ? "" : " and e.actor = :actor") +
            (start == null ? "" : " and e.createdAt >= :start") +
            (end == null ? "" : " and e.createdAt < :end") +
            " order by e.createdAt desc, e.id desc";
        TypedQuery<CampaignEvent> typed = entityManager.createQuery(query, CampaignEvent.class)
            .setParameter("description", description);
        bindFilters(typed, actor, start, end);
        return typed.setMaxResults(LIMIT_PER_SOURCE).getResultList();
    }

    private static void bindFilters(TypedQuery<?> query, String actor,
                                    LocalDateTime start, LocalDateTime end) {
        if (actor != null) query.setParameter("actor", actor);
        if (start != null) query.setParameter("start", start);
        if (end != null) query.setParameter("end", end);
    }
}
