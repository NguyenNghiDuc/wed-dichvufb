package com.example.fbservice.admin;

import com.example.fbservice.campaign.CampaignAssignmentEvent;
import com.example.fbservice.campaign.CampaignEvent;
import com.example.fbservice.catalog.ServiceCatalogAudit;
import jakarta.persistence.EntityManager;
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
    public enum Area { ALL, ACCOUNTS, CATALOG, ASSIGNMENTS, ORDER_STATUS }

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
        if (area == null) throw new IllegalArgumentException("Invalid activity category");
        List<Entry> entries = new ArrayList<>();
        if (area == Area.ALL || area == Area.ACCOUNTS) {
            entityManager.createQuery(
                "select e from AdminAccountAudit e order by e.createdAt desc, e.id desc",
                AdminAccountAudit.class)
                .setMaxResults(LIMIT_PER_SOURCE).getResultList().forEach(e ->
                    entries.add(new Entry(Area.ACCOUNTS, e.getActor(),
                        "Tài khoản #" + e.getTargetUserId(),
                        e.getOperation() + ": " + e.getBeforeValue() + " → " + e.getAfterValue(),
                        e.getCreatedAt(), e.getId())));
        }
        if (area == Area.ALL || area == Area.CATALOG) {
            entityManager.createQuery(
                "select e from ServiceCatalogAudit e order by e.createdAt desc, e.id desc",
                ServiceCatalogAudit.class)
                .setMaxResults(LIMIT_PER_SOURCE).getResultList().forEach(e ->
                    entries.add(new Entry(Area.CATALOG, e.getActor(), "Dịch vụ " + e.getServiceCode(),
                        "Báo giá: " + e.getOldPrice() + " → " + e.getNewPrice() + " VND; mở bán: " +
                            e.isOldEnabled() + " → " + e.isNewEnabled(),
                        e.getCreatedAt(), e.getId())));
        }
        if (area == Area.ALL || area == Area.ASSIGNMENTS) {
            entityManager.createQuery(
                "select e from CampaignAssignmentEvent e order by e.createdAt desc, e.id desc",
                CampaignAssignmentEvent.class)
                .setMaxResults(LIMIT_PER_SOURCE).getResultList().forEach(e ->
                    entries.add(new Entry(Area.ASSIGNMENTS, e.getActor(),
                        "Chiến dịch #" + e.getCampaignId(),
                        "Phân công: " + (e.getOldAssignee() == null ? "Chưa giao" : e.getOldAssignee()) +
                        " → " + (e.getNewAssignee() == null ? "Chưa giao" : e.getNewAssignee()),
                        e.getCreatedAt(), e.getId())));
        }
        if (area == Area.ALL || area == Area.ORDER_STATUS) {
            // Customer-initiated and staff-initiated campaign events are not Admin actions.
            entityManager.createQuery(
                "select e from CampaignEvent e " +
                "where e.description = :adminDescription order by e.createdAt desc, e.id desc",
                CampaignEvent.class)
                .setParameter("adminDescription", "Quản trị viên cập nhật trạng thái")
                .setMaxResults(LIMIT_PER_SOURCE).getResultList().forEach(e ->
                    entries.add(new Entry(Area.ORDER_STATUS, e.getActor(),
                        "Chiến dịch #" + e.getCampaignId(),
                        "Trạng thái: " + (e.getOldStatus() == null ? "—" : e.getOldStatus().name()) +
                            " → " + e.getNewStatus().name(),
                        e.getCreatedAt(), e.getId())));
        }
        return entries.stream()
            .sorted(Comparator.comparing(Entry::createdAt).reversed()
                .thenComparing(Entry::area)
                .thenComparing(Comparator.comparingLong(Entry::sourceId).reversed()))
            .limit(DISPLAY_LIMIT).toList();
    }
}
