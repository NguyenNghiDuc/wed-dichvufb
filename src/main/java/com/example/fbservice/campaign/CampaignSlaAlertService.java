package com.example.fbservice.campaign;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Reads max 100 unresolved overdue orders per run; safe for large tables. */
@Service
public class CampaignSlaAlertService {
    public static final List<CampaignStatus> ACTIVE = List.of(
        CampaignStatus.PENDING_REVIEW, CampaignStatus.APPROVED, CampaignStatus.IN_PROGRESS);
    private final CampaignOrderRepository orders;
    private final CampaignSlaDelivery delivery;

    public CampaignSlaAlertService(CampaignOrderRepository orders, CampaignSlaDelivery delivery) {
        this.orders=orders;this.delivery=delivery;
    }

    public int scanAt(LocalDateTime utcNow) {
        if(utcNow==null) throw new IllegalArgumentException("Time required");
        var batch=orders.findByStatusInAndDueAtBeforeAndOverdueNotifiedAtIsNull(ACTIVE,utcNow,
            PageRequest.of(0,100,Sort.by(Sort.Order.asc("dueAt"),Sort.Order.asc("id"))));
        int sent=0;
        for(CampaignOrder order:batch.getContent())
            if(delivery.deliverIfOverdue(order.getId(),utcNow)) sent++;
        return sent;
    }
    public int scanNow(){return scanAt(LocalDateTime.now(ZoneOffset.UTC));}
}
