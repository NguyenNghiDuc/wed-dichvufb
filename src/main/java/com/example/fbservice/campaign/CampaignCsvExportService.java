package com.example.fbservice.campaign;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bounded CSV export with spreadsheet formula-injection mitigation. */
@Service
public class CampaignCsvExportService {
    private static final int CHUNK_SIZE = 500;
    private static final int MAX_ROWS = 5000;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final CampaignOrderRepository orders;
    private final CampaignOrderService customerOrders;

    public CampaignCsvExportService(CampaignOrderRepository orders, CampaignOrderService customerOrders) {
        this.orders = orders;
        this.customerOrders = customerOrders;
    }

    @Transactional(readOnly = true)
    public String exportLatest() {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("id,owner,platform,service,status,price_vnd,created_at,target_url,brief\r\n");
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        int exported = 0;
        int pageNumber = 0;
        while (exported < MAX_ROWS) {
            Page<CampaignOrder> page = orders.findAll(PageRequest.of(pageNumber, CHUNK_SIZE, sort));
            List<CampaignOrder> rows = page.getContent();
            if (rows.isEmpty()) break;
            for (CampaignOrder order : rows) {
                if (exported >= MAX_ROWS) break;
                appendRow(csv,
                    order.getId(), order.getOwnerUsername(), order.getPlatform(),
                    order.getServiceType(), order.getStatus(), order.getQuotedPriceVnd(),
                    DATE.format(order.getCreatedAt()), order.getTargetUrl(), order.getBrief());
                exported++;
            }
            if (!page.hasNext()) break;
            pageNumber++;
        }
        return csv.toString();
    }

    /** Export only the authenticated customer's rows, respecting the same filters as the list. */
    @Transactional(readOnly = true)
    public String exportMine(String username, Long orderId, CampaignStatus status) {
        return exportMine(username, orderId, status, null, null, null);
    }

    @Transactional(readOnly = true)
    public String exportMine(String username, Long orderId, CampaignStatus status,
                             CampaignServiceType serviceType, LocalDate from, LocalDate to) {
        return exportMine(username, orderId, status, serviceType, from, to, null);
    }

    @Transactional(readOnly = true)
    public String exportMine(String username, Long orderId, CampaignStatus status,
                             CampaignServiceType serviceType, LocalDate from, LocalDate to, String q) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("id,platform,service,status,price_vnd,created_at,target_url,brief\r\n");
        int exported = 0;
        int pageNumber = 0;
        while (exported < MAX_ROWS) {
            // pageMine applies ownership in the database query, validates the filters,
            // and caps each batch at 50 rows; do not fetch all users and filter in Java.
            Page<CampaignOrder> page = customerOrders.pageMine(username, pageNumber, 50, orderId, status, serviceType, from, to, q);
            if (page.isEmpty()) break;
            for (CampaignOrder order : page.getContent()) {
                if (exported >= MAX_ROWS) break;
                appendRow(csv,
                    order.getId(), order.getPlatform(), order.getServiceType(), order.getStatus(),
                    order.getQuotedPriceVnd(), DATE.format(order.getCreatedAt()),
                    order.getTargetUrl(), order.getBrief());
                exported++;
            }
            if (!page.hasNext()) break;
            pageNumber++;
        }
        return csv.toString();
    }

    private void appendRow(StringBuilder csv, Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i != 0) csv.append(',');
            csv.append(escape(cells[i]));
        }
        csv.append("\r\n");
    }

    static String escape(Object value) {
        String raw = value == null ? "" : value.toString();
        String trimmed = raw.stripLeading();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
            raw = "'" + raw;
        } else if (!trimmed.isEmpty() && (trimmed.charAt(0) == '\t' || trimmed.charAt(0) == '\r')) {
            raw = "'" + raw;
        }
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }
}
