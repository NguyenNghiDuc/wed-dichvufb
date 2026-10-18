package com.example.fbservice.campaign;

import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer review CSV: owner-filtered database paging with bounded memory
 * and spreadsheet formula protection. No user IDs or internal actors exported.
 */
@Service
public class CustomerReviewCsvExportService {
    private static final int CHUNK = 50;
    private static final int MAX_ROWS = 5000;
    private static final DateTimeFormatter TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CampaignReviewService reviews;

    public CustomerReviewCsvExportService(CampaignReviewService reviews) {
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public String exportMine(String username, Integer rating, String query) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("campaign_id,rating,feedback,created_at\r\n");
        int count = 0;
        for (int p = 0; count < MAX_ROWS; p++) {
            // Every page is SQL-scoped to the authenticated account, same
            // filters/sort as customer review history. Max size is 50.
            Page<CampaignReviewView> page = reviews.pageForOwner(
                username, p, CHUNK, rating, query);
            if (page.isEmpty()) break;
            for (CampaignReviewView entry : page.getContent()) {
                if (count >= MAX_ROWS) break;
                csv.append(CampaignCsvExportService.escape(entry.campaignId())).append(',')
                   .append(CampaignCsvExportService.escape(entry.rating())).append(',')
                   .append(CampaignCsvExportService.escape(entry.feedback())).append(',')
                   .append(CampaignCsvExportService.escape(TIME.format(entry.createdAt())))
                   .append("\r\n");
                count++;
            }
            if (!page.hasNext()) break;
        }
        return csv.toString();
    }
}
