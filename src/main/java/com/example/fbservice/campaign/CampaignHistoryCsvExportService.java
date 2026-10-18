package com.example.fbservice.campaign;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bounded, owner-only export of a single campaign's status changes. */
@Service
public class CampaignHistoryCsvExportService {
    private static final int PAGE_SIZE = 50;
    private static final int MAX_ROWS = 1000;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final CampaignOrderService orders;

    public CampaignHistoryCsvExportService(CampaignOrderService orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public String export(String owner, Long campaignId) {
        // Check ownership even for an empty history. An unknown or foreign ID always returns 404.
        orders.findMineById(owner, campaignId);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("created_at,previous_status,new_status,description\r\n");
        int count = 0;
        int pageNumber = 0;
        while (count < MAX_ROWS) {
            Page<CampaignHistoryEntry> page = orders.pageHistoryForOwner(
                owner, campaignId, pageNumber, PAGE_SIZE);
            List<CampaignHistoryEntry> entries = page.getContent();
            if (entries.isEmpty()) break;
            for (CampaignHistoryEntry e : entries) {
                if (count >= MAX_ROWS) break;
                append(csv, FORMAT.format(e.createdAt()), e.oldStatus() == null ? "" : e.oldStatus().name(),
                    e.newStatus().name(), e.description());
                count++;
            }
            if (!page.hasNext()) break;
            pageNumber++;
        }
        return csv.toString();
    }

    private static void append(StringBuilder csv, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) csv.append(',');
            csv.append(CampaignCsvExportService.escape(values[i]));
        }
        csv.append("\r\n");
    }
}
