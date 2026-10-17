package com.example.fbservice.admin;

import com.example.fbservice.campaign.CampaignOrder;
import com.example.fbservice.campaign.CampaignStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Read-only priority queues, with SQL-side pagination and no automatic mutations. */
@Service
public class AdminAttentionQueueService {
    public enum Queue {
        OVERDUE_PENDING("Chờ duyệt trên 48 giờ"),
        OVERDUE_IN_PROGRESS("Đang xử lý quá 7 ngày"),
        READY_TO_ASSIGN("Đã duyệt, chưa phân công"),
        REJECTED("Đã từ chối");
        private final String label;
        Queue(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public record Result(List<CampaignOrder> rows, boolean hasNext, int page, Queue queue) {}

    private final EntityManager em;

    public AdminAttentionQueueService(EntityManager em) {
        this.em = em;
    }

    @Transactional(readOnly = true)
    public Result find(Queue queue, int page) {
        if (queue == null || page < 0 || page > 10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid queue or page");
        String criteria = switch (queue) {
            case OVERDUE_PENDING -> "c.status = :status and c.createdAt < :cutoff";
            case OVERDUE_IN_PROGRESS -> "c.status = :status and c.updatedAt < :cutoff";
            case READY_TO_ASSIGN -> "c.status = :status and c.assigneeUsername is null";
            case REJECTED -> "c.status = :status";
        };
        TypedQuery<CampaignOrder> query = em.createQuery(
            "select c from CampaignOrder c where " + criteria +
            " order by c.createdAt asc, c.id asc", CampaignOrder.class);
        CampaignStatus status = switch (queue) {
            case OVERDUE_PENDING -> CampaignStatus.PENDING_REVIEW;
            case OVERDUE_IN_PROGRESS -> CampaignStatus.IN_PROGRESS;
            case READY_TO_ASSIGN -> CampaignStatus.APPROVED;
            case REJECTED -> CampaignStatus.REJECTED;
        };
        query.setParameter("status", status);
        if (queue == Queue.OVERDUE_PENDING)
            query.setParameter("cutoff", LocalDateTime.now().minusHours(48));
        if (queue == Queue.OVERDUE_IN_PROGRESS)
            query.setParameter("cutoff", LocalDateTime.now().minusDays(7));
        // Fetch one extra row to determine whether another page exists, without a full count.
        List<CampaignOrder> fetched = query.setFirstResult(page * 20)
            .setMaxResults(21).getResultList();
        boolean hasNext = fetched.size() > 20;
        return new Result(List.copyOf(fetched.subList(0, Math.min(fetched.size(), 20))),
            hasNext, page, queue);
    }
}
