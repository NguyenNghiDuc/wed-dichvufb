package com.example.fbservice.admin;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Bounded aggregate CSV: no customer PII and no financial ledger assumptions. */
@RestController
public class AdminOperationsReportController {
    private final AdminDashboardService dashboard;

    public AdminOperationsReportController(AdminDashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping(value = "/admin/reports/overview.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportOverview() {
        AdminDashboardService.Metrics metrics = dashboard.metrics();
        AdminDashboardService.OperationsSummary operations = dashboard.operations();
        StringBuilder out = new StringBuilder("\uFEFF");
        out.append("metric,value\r\n")
            .append("total_users,").append(metrics.totalUsers()).append("\r\n")
            .append("enabled_users,").append(metrics.enabledUsers()).append("\r\n")
            .append("total_campaigns,").append(metrics.totalCampaigns()).append("\r\n")
            .append("pending_campaigns,").append(metrics.pendingCampaigns()).append("\r\n")
            .append("open_tickets,").append(metrics.openTickets()).append("\r\n")
            .append("active_pipeline_quotes_vnd,").append(operations.pipelineQuotedValueVnd()).append("\r\n")
            .append("\r\nstatus,order_count,quoted_value_vnd\r\n");
        // Enum constant values, never external text: no spreadsheet formulas/PII.
        for (AdminStatusBreakdown row : operations.statuses()) {
            out.append(row.status().name()).append(',')
                .append(row.orderCount()).append(',')
                .append(row.quotedValueVnd()).append("\r\n");
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admin-operations.csv\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(out.toString().getBytes(StandardCharsets.UTF_8));
    }
}
