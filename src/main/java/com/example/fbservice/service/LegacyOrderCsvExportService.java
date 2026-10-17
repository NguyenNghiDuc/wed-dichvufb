package com.example.fbservice.service;

import com.example.fbservice.model.Order;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacyOrderCsvExportService {
    private static final int PAGE_SIZE = 50;
    private static final int MAX_ROWS = 5000;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final OrderService orders;

    public LegacyOrderCsvExportService(OrderService orders) {
        this.orders = orders;
    }

    @Transactional(readOnly=true)
    public String exportMine(String owner) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("id,service,link,quantity,unit_price_vnd,total_price_vnd,note,created_at\r\n");
        int exported = 0;
        int page = 0;
        while (exported < MAX_ROWS) {
            Page<Order> result = orders.pageMine(owner, page, PAGE_SIZE);
            List<Order> records = result.getContent();
            if (records.isEmpty()) break;
            for (Order o : records) {
                if (exported >= MAX_ROWS) break;
                append(csv, o.getId(), o.getServerName(), o.getLinkOrder(), o.getQuantity(),
                    o.getPricePerUnit(), o.getTotalPrice(), o.getNote(),
                    o.getCreatedAt()==null ? "" : DATE.format(o.getCreatedAt()));
                exported++;
            }
            if (!result.hasNext()) break;
            page++;
        }
        return csv.toString();
    }

    private static void append(StringBuilder csv, Object... values) {
        for (int i=0; i<values.length; i++) {
            if (i>0) csv.append(',');
            csv.append(escape(values[i] == null ? "" : values[i].toString()));
        }
        csv.append("\r\n");
    }

    static String escape(String raw) {
        String trimmed = raw.stripLeading();
        boolean leadingControl = !raw.isEmpty() && (raw.charAt(0)=='\t' ||
            raw.charAt(0)=='\r' || raw.charAt(0)=='\n');
        if (leadingControl || (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0))>=0)) {
            raw = "'" + raw;
        }
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }
}
